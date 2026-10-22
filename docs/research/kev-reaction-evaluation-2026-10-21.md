# Kev-4B reaction evaluation

Reviewed and measured locally on 2026-10-21 for
[#188](https://github.com/Quzzar/kithkyn/issues/188) and
[#191](https://github.com/Quzzar/kithkyn/issues/191). The pinned local endpoint worked, but
this experiment does not justify making it the default: its fixed API-confidence gate of 0.8
accepted none of the supplied reactions. The fixture and its near-duplicate split do not
establish generalization or calibrated game behavior.

## Pins and provenance

| Input | Immutable revision |
| --- | --- |
| [Kev runtime](https://github.com/jaredpalmer/kev/tree/1f04cfa28e6a708485b28a9585010b9e436b776e) | `1f04cfa28e6a708485b28a9585010b9e436b776e` |
| [jaredpalmer/kev-4b](https://huggingface.co/jaredpalmer/kev-4b/tree/139fdd94f1b6a6ad80cc15e08fcb99cac885a101) | `139fdd94f1b6a6ad80cc15e08fcb99cac885a101` |
| [Qwen/Qwen3.5-4B-Base](https://huggingface.co/Qwen/Qwen3.5-4B-Base/tree/1001bb4d826a52d1f399e183466143f4da7b741b) | `1001bb4d826a52d1f399e183466143f4da7b741b` |

The current Kev `v1.0` tag resolves to `6cfce5c2fa4b4bd64026336ab649c5ca78857d52`, whose
adapter and head hashes match the published checkpoint above. The
[training configuration](https://huggingface.co/jaredpalmer/kev-4b/blob/139fdd94f1b6a6ad80cc15e08fcb99cac885a101/training_config.json)
pins the required base revision. The checkpoint is a LoRA adapter and pointer head, not a
self-contained generative GGUF. Both model repositories and the runtime declare Apache-2.0.

## Artifacts

| File | Bytes | SHA-256 |
| --- | ---: | --- |
| Kev `adapter_model.safetensors` | 129,924,032 | `90e817356246e7f18bfa7ca3d31794cd4fbeb3332a66a84cb51d9ceae925f2b2` |
| Kev `head.pt` | 5,249,791 | `dd633435998ecc751ac538717a3742e32149500fabf7d7276287dbf0693f347c` |
| Qwen `model.safetensors-00001-of-00002.safetensors` | 5,329,398,712 | `df547074dce70532a0493e5433152bd17a65efb89088cfabc2e7e2371a93d712` |
| Qwen `model.safetensors-00002-of-00002.safetensors` | 3,990,429,344 | `590fbaac095dd31db886c322d9d2f7df47777966391acf306ddddc3e4e3a15ef` |

Sources: [Kev Hub metadata](https://huggingface.co/api/models/jaredpalmer/kev-4b/revision/139fdd94f1b6a6ad80cc15e08fcb99cac885a101?blobs=true),
[Qwen Hub metadata](https://huggingface.co/api/models/Qwen/Qwen3.5-4B-Base/revision/1001bb4d826a52d1f399e183466143f4da7b741b?blobs=true).
The official loader's matching files total **159,702,543 bytes for Kev** and
**9,342,808,116 bytes for Qwen**, approximately **9.503 GB**, excluding Python packages.
These include the required configs, tokenizer data, two base shards, adapter, and head.
All 23 files are now downloaded at those pins and verified against the published byte lengths.
Actual SHA-256 values were computed for every file; every available publisher LFS SHA-256,
including the four weight artifacts above, matched. The complete local record is
`run/kev-evaluation/staging-manifest.json`.

## Local setup

All experiment files are confined to the ignored directory:

`/Users/quzzar/.codex/worktrees/npc-agency/kithkyn/run/kev-evaluation`

- Runtime: `runtime/`, checked out at the pinned commit.
- Interpreter: `runtime/.venv/bin/python`, Python 3.13.12.
- Model cache: `huggingface/hub/`, with immutable checkpoint/base snapshot directories.
- Download/verification script: `stage_weights.py`.
- Actual byte/hash verification record: `staging-manifest.json`.
- Generated request schema: `api-schema.json`; synthetic parser/serializer check:
  `api-schema-fixture.json`.
- Dependency cache: `uv-cache/`.

Frozen dependency installation succeeded with the official `serve` extra. Relevant locked
versions are Torch 2.8.0, Transformers 5.17.0, PEFT 0.21.0, mlx-lm 0.31.3, MLX 0.32.2,
TypeSafe SDK 0.6.0, FastAPI 0.141.1, and Uvicorn 0.53.0. The
[project file](https://github.com/jaredpalmer/kev/blob/1f04cfa28e6a708485b28a9585010b9e436b776e/pyproject.toml)
requires Python >=3.12,<3.14 and supplies the Apple Silicon MLX dependency in the serve extra.

**Run status:** runtime setup, model loading, endpoint verification, and the authorized replay
are complete. The server shut down after replay, Uvicorn logged application shutdown complete,
and localhost port 8008 stopped listening. The GPU is released; cached artifacts remain.

The request parser and response serializer passed a local CPU-only fixture with synthetic
probabilities. This verifies the schema example below, not model inference or HTTP serving.

## Actual HTTP contract

The pinned [api.py](https://github.com/jaredpalmer/kev/blob/1f04cfa28e6a708485b28a9585010b9e436b776e/kev/api.py)
defines `POST /v1/systemone`. A minimal choice request is:

```json
{
  "model": "kev-latest",
  "state": {"situation": "The resident sees smoke nearby."},
  "questions": {
    "reaction": {
      "type": "choice",
      "instructions": "Select one offered reaction using the supplied facts.",
      "criteria": {"continue": "Continue the current activity", "warn": "Warn nearby residents"}
    }
  }
}
```

This is a schema example, not a record of an inference. A successful body has:

```json
{
  "model": "kev-latest",
  "answers": {
    "reaction": {
      "type": "choice",
      "choice": "warn",
      "confidence": 0.6,
      "probabilities": {"continue": 0.2, "warn": 0.8}
    }
  },
  "usage": {"input_tokens": 100, "output_tokens": 30},
  "latency_ms": 120.0
}
```

The response above is illustrative. Choice keys are supplied by the caller, with 1 to 255
options. `state` and instructions may be JSON values but are rendered as labelled text.
`noul` returns a yes probability under `noul`; `score` takes an ordered criteria list and
returns an expected index with probabilities and a legend. Neither returns generated prose.

`confidence` for a choice is `(p_max - 1/K) / (1 - 1/K)`, or 1 for one option. It is not
the winning raw probability. Probabilities and confidence are rounded to four decimal places.
Do not require a mathematically exact sum of one after serialization.

The pinned [serve.py](https://github.com/jaredpalmer/kev/blob/1f04cfa28e6a708485b28a9585010b9e436b776e/kev/serve.py)
adds usage and `latency_ms`. The latter excludes queue wait. Bearer authorization is checked
when `KEV_API_KEY` is set. Schema/admission errors return 422; invalid configured credentials
return 401. A stopped model queue returns 503. With truncation disabled, excessive context
fails explicitly instead of being shortened.

The response's `model` field echoes the request. It does not attest to loaded weights.
`GET /v1/models` reports the actual `run`, base, backend, dtype, temperature, and cache stats.
Verify those against the experiment manifest. The runtime calls this decision API directly;
it does not provide an interchangeable `/v1/chat/completions` implementation.

## Limits and interpretation

The author reports When2Call development accuracy 0.660 for 4B, not a guarantee for game
reactions. Calibration is one fitted temperature, approximately 2.41; quality varies by
domain, option order can change answers, and only 8,192 tokens are validated despite a
65,536-token serving limit. None establishes correct NPC behavior.

On an M5 with 32 GB, the published 8k test used an 11.9 GB process footprint and took
6.6 seconds for a new state versus 354 ms cached. The CUDA report uses 14.3 GB resident
memory. These are author measurements, not results from this experiment.
[Model card](https://huggingface.co/jaredpalmer/kev-4b)

Kithkyn must construct the legal offered set and retain captured references, then validate the
selected key and current eligibility on the game thread. A confidence threshold needs a
labelled game-specific evaluation; it must not convert a high score into permission for an
unimplemented or stale action. Authored dialogue remains with the generative provider.

## Local results

Startup took **30.362 seconds**. `/v1/models` confirmed the pinned run and expected base,
`backend=mlx`, `dtype=bfloat16`, `device=mps`, temperature **2.406050072164233**, and
truncation disabled. A separate generic billing-classification smoke request took 3,629 ms
wall time initially and 93.8 ms on an immediate repeat. Those two calls are not reaction scores.

The parent supplied **48 exact Java-built requests: 16 synthetic states, three criteria orders
per state**. Input SHA-256:
`ccb19fa59c03a2236c11454e20c8789c77697b6ad6f2c80eaf2e5e3497e2690a`.
They were sent sequentially without modifying payloads or criteria insertion order, with no
retries. Neither threshold nor prompt was tuned on the results. The corpus was generated
before subsequent native target/guard checks were tightened; this replay does not exercise
those checks or physical action completion.

**The `held_out` rows only perturb coordinates/distance by one block. They are near-duplicate
checks, not independent unseen scenarios and not valid generalization or calibration
evidence.** Each split has eight states repeated in three orders, not 24 independent examples.
Expected sets are authored acceptable reactions, not objective combat optimality.

| Metric | Calibration | Near-duplicate `held_out` |
| --- | ---: | ---: |
| Valid HTTP/schema/distribution/confidence | 24/24 | 24/24 |
| Choice in authored acceptable set | 19/24 (79.2%) | 19/24 (79.2%) |
| API confidence >= 0.8: accepted | 0/24 | 0/24 |
| API confidence >= 0.8: accepted-case error | Undefined, no accepted cases | Undefined, no accepted cases |
| Selected raw probability >= 0.8: accepted | 3/24 (12.5%) | 3/24 (12.5%) |
| Raw-probability gate: accepted-case label errors | 0/3 | 0/3 |
| Same choice across all three orders | 6/8 states | 6/8 states |

The two gates are different quantities. Every raw-probability-accepted choice was `continue`,
so neither gate admitted an optional reaction in this fixture. A zero-error claim without
coverage would therefore be misleading. Validation checked offered keys, exact probability
keys, finite bounded values, rounded normalization, argmax consistency, and the documented
chance-corrected confidence formula. All 48 passed; maximum formula deviation from rounded
probabilities was 0.000116, within the fixed 0.0002 rounding tolerance.

Overall label agreement was **38/48**. Median HTTP wall time was **379 ms**, p95 **980 ms**
(nearest-rank), and maximum **3,423 ms**. The 16 first requests for a state had median
**821 ms**; the 32 subsequent reordered requests for the same states had median **343 ms**.
The endpoint's final counters confirm 17 misses and 33 hits including the two smoke calls,
50 batches/requests, and no OOM retries. Payloads used 403 to 477 reported input tokens.
These results mix first-use compilation and prefix caching; they are not a comparison with
the generative models or a Minecraft frame-time measurement.

Native macOS `vmmap` reports a **9.6G physical footprint after replay and 15.6G lifetime
peak**. Earlier sampled RSS omitted shared graphics allocations and is not the total memory
figure. Before replay, the native report included approximately 8.0G of IOAccelerator graphics
allocations. Reports are saved before and after replay.

### Choices and API confidence by order

Each cell lists orders 0, 1, and 2. The full raw responses retain every probability.

| State profile | Calibration | Near-duplicate `held_out` |
| --- | --- | --- |
| curious | investigate (0.1485), investigate (0.2207), investigate (0.1872) | investigate (0.1494), investigate (0.2280), investigate (0.1907) |
| secluded | continue (0.3280), continue (0.3322), continue (0.3592) | continue (0.3408), continue (0.3438), continue (0.3803) |
| protective | seek_help (0.4640), seek_help (0.4286), seek_help (0.4300) | seek_help (0.4452), seek_help (0.4174), seek_help (0.4195) |
| cautious | retreat (0.4164), retreat (0.4803), retreat (0.4795) | retreat (0.4094), retreat (0.4833), retreat (0.4820) |
| bold_guard | continue (0.1183), defend (0.2373), defend (0.1468) | continue (0.1167), defend (0.2399), defend (0.1492) |
| cautious_guard | continue (0.1273), retreat (0.1373), retreat (0.1478) | continue (0.1315), retreat (0.1341), retreat (0.1403) |
| essential_warn | continue (0.2217), continue (0.3028), continue (0.3028) | continue (0.2220), continue (0.3021), continue (0.3021) |
| essential_continue | continue (0.6156), continue (0.6293), continue (0.6293) | continue (0.6191), continue (0.6258), continue (0.6258) |

Curious/secluded and protective/cautious pairs differed in all three orders in each split,
with both choices accepted by the authored labels. Both guard profiles chose `continue` in
order 0, then differentiated into `defend` and `retreat` in orders 1 and 2. The essential-post
pair always chose `continue`, missing the authored `warn` preference. Across both splits,
16/24 matched-order pair comparisons differed and had both labels acceptable. Four of 16
states changed choice solely when option order changed. This is evidence of some sensitivity
to character facts and a material order effect, not robust person-level judgment.

### Reproducible records

The preserved [raw replay](../../tools/llm/results/2026-10-21/kev-reactions-raw.jsonl)
contains all 48 requests and responses, including the exact original source lines. The
[analysis](../../tools/llm/results/2026-10-21/kev-reactions-analysis.json) contains every
scored row and aggregate. The standard-library-only
[analyzer](../../tools/llm/analyze_reactions.py) verifies the fixed corpus hash and accepts
explicit input/output paths:

```sh
python3 tools/llm/analyze_reactions.py --raw tools/llm/results/2026-10-21/kev-reactions-raw.jsonl --output /tmp/kev-reactions-analysis.json
```

Run that from the repository root. An optional `--source PATH` also verifies the original
request JSONL against source lines embedded in the raw records. Recalculation does not
start the model, use the GPU, require downloaded weights, or fit a threshold.

Additional ignored experiment records remain under `run/kev-evaluation/`:

- `reaction-requests.jsonl`: supplied requests and authored labels.
- `reaction-raw.jsonl`: exact source lines, HTTP bodies, raw responses, IDs, split/pair,
  expected labels, criteria order, HTTP status, and wall time.
- `reaction-replay-manifest.json`: fixed threshold, source hash, and run times.
- `reaction-analysis.json`: every scored row, confidence/distribution checks, split/order
  summaries, state choices, and pair comparisons.
- The sequential runner is now maintained at `tools/llm/replay_reactions.py`, alongside
  `tools/llm/analyze_reactions.py`; its historical outputs remain unchanged.
- `startup.json`, `models-after-replay.json`, `endpoint-smoke.json`, `kev-server.log`, and
  `memory-before-replay.txt` / `memory-after-replay.txt`: runtime and memory evidence.

Raw response SHA-256:
`7be76757e610120d35e3276ee598f009e35dc1189685e06ba8eede89b4e8ca42`.

No default or threshold change is supported by this small,
near-duplicate fixture. A broader labelled corpus with independent scenarios and actual game
outcomes would be needed before interpreting confidence as an acceptance policy.

## Reproduction command

For a future explicitly authorized run, use the same isolated cache, offline inputs, and
explicit backend:

```sh
HF_HOME=/Users/quzzar/.codex/worktrees/npc-agency/kithkyn/run/kev-evaluation/huggingface XDG_CACHE_HOME=/Users/quzzar/.codex/worktrees/npc-agency/kithkyn/run/kev-evaluation/cache HF_HUB_OFFLINE=1 KEV_BACKEND=mlx KEV_DTYPE=bf16 /Users/quzzar/.codex/worktrees/npc-agency/kithkyn/run/kev-evaluation/runtime/.venv/bin/python -m kev.serve --device mps --host 127.0.0.1 --port 8008 --run jaredpalmer/kev-4b@139fdd94f1b6a6ad80cc15e08fcb99cac885a101
```

The installed editable package resolves the runtime from the absolute interpreter path.
Do not silently select a different model,
backend, precision, or endpoint after an error. Confirm `/v1/models`, then replay the supplied
labelled requests, measuring HTTP wall time, compute time, cold/cache behavior, confidence,
choice correctness, and legality. Only measured results should decide whether this backend
is useful for the reaction path.

The later #189 undertaking rework adds recorded outcomes to live reaction context. The
48-response replay above preserves the earlier request schema and revision; its scores
are historical evidence, not a measurement of that revised prompt.
