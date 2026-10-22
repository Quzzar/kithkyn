"""Send the parent-supplied reaction corpus sequentially without changing payload order."""
from __future__ import annotations

import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import time
import urllib.error
import urllib.request

def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--endpoint", default="http://127.0.0.1:8008/v1/systemone")
    args = parser.parse_args()
    source = args.source
    raw_lines = source.read_text().splitlines()
    inputs = [json.loads(line) for line in raw_lines]
    if not inputs or len({row["id"] for row in inputs}) != len(inputs):
        raise RuntimeError("Expected nonempty uniquely identified requests")
    output = args.output
    output.parent.mkdir(parents=True, exist_ok=True)
    if output.exists():
        raise RuntimeError("Refusing to overwrite an existing replay result")
    manifest = {
        "started_at": datetime.now(timezone.utc).isoformat(),
        "source": str(source), "source_sha256": hashlib.sha256(source.read_bytes()).hexdigest(),
        "requests": len(inputs), "endpoint": args.endpoint,
        "threshold": 0.8, "sequential": True, "retries": 0,
    }
    output.with_suffix(".manifest.json").write_text(json.dumps(manifest, indent=2) + "\n")
    with output.open("x") as target:
        for index, (raw, row) in enumerate(zip(raw_lines, inputs), start=1):
            payload = row["payload"]
            body = json.dumps(payload, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
            request = urllib.request.Request(manifest["endpoint"], data=body,
                                             headers={"Content-Type": "application/json"}, method="POST")
            started = time.perf_counter()
            failure = None
            try:
                with urllib.request.urlopen(request, timeout=5) as response:
                    status = response.status
                    response_text = response.read().decode("utf-8")
            except urllib.error.HTTPError as error:
                status = error.code
                response_text = error.read().decode("utf-8", errors="replace")
                failure = f"HTTP {status}"
            except (urllib.error.URLError, TimeoutError, OSError) as error:
                status, response_text, failure = None, "", str(error)
            result = {
                "id": row["id"], "split": row["split"], "pair": row.get("pair"),
                "expected": row["expected"], "source_line": raw,
                "request_body": body.decode("utf-8"),
                "criteria_order": list(payload["questions"]["reaction"]["criteria"]),
                "http_status": status, "wall_ms": round((time.perf_counter() - started) * 1000, 3),
                "response_body": response_text, "transport_error": failure,
            }
            target.write(json.dumps(result, ensure_ascii=False) + "\n")
            target.flush()
            print(json.dumps({"completed": index, "id": row["id"], "status": status,
                              "wall_ms": result["wall_ms"]}), flush=True)
            if failure:
                raise RuntimeError(f"Replay stopped without retry or substitution: {row['id']}: {failure}")
    manifest["completed_at"] = datetime.now(timezone.utc).isoformat()
    output.with_suffix(".manifest.json").write_text(json.dumps(manifest, indent=2) + "\n")


if __name__ == "__main__":
    main()
