# Local Kev evaluation preparation

Reviewed 2026-10-21 for [#191](https://github.com/Quzzar/kithkyn/issues/191). This records
verified artifacts and a deferred experiment, not a successful local evaluation.

## Decision

**Kev-0.8B can be evaluated locally on this Mac after environment setup, but it is not a
validated action router.** The author explicitly excludes tool-call routing: its When2Call
test accuracy is 0.133, below four-way chance. Bounded game decisions still need their own
labels and replay tests. This prevents treating small size or fast inference as evidence of
good NPC judgment. [Model card](https://huggingface.co/jaredpalmer/kev-0.8b)

Kev scores supplied choices; it does not generate dialogue, reasons, or arbitrary arguments.
It is a separate decision capability, not another model for llama.cpp's chat-completion path.

## Exact checkpoint and download

The smallest checkpoint in the author's current public family is `jaredpalmer/kev-0.8b`.
Pin model revision **`bf75a6a8848ea6960ff2ed108d9ed44c2941174f`** and runtime commit
**`1f04cfa28e6a708485b28a9585010b9e436b776e`**. The Hub's `v1.0` tag currently resolves to
that model revision. The older card revision `9a45d25eb2ab761841196625383fa1dff0e56c1e`
contains the same adapter/head hashes. Prefer immutable commits over aliases.

The checkpoint is an adapter and pointer head, not a complete 45 MB model. Its
[training configuration](https://huggingface.co/jaredpalmer/kev-0.8b/blob/bf75a6a8848ea6960ff2ed108d9ed44c2941174f/training_config.json)
pins `Qwen/Qwen3.5-0.8B-Base` at **`dc7cdfe2ee4154fa7e30f5b51ca41bfa40174e68`**.

| Artifact | Bytes | Publisher-hosted SHA-256 |
| --- | ---: | --- |
| Kev `adapter_model.safetensors` | 43,338,624 | `9b908623acb162118575f4e7a94524f9c139c335be4bfb74d6cfceca01e1885a` |
| Kev `head.pt` | 2,103,999 | `f400bd12802b2b105ae45d6b03774a158a3db4fccff42413734ddca2e5c920b6` |
| Qwen `model.safetensors-00001-of-00001.safetensors` | 1,746,942,600 | `c2b1e5a17d9c1e27685d92ed9b382911ebb99955ecd89052d1721241adfbab6c` |

Sources: [pinned Kev metadata](https://huggingface.co/api/models/jaredpalmer/kev-0.8b/revision/bf75a6a8848ea6960ff2ed108d9ed44c2941174f?blobs=true),
[pinned base metadata](https://huggingface.co/api/models/Qwen/Qwen3.5-0.8B-Base/revision/dc7cdfe2ee4154fa7e30f5b51ca41bfa40174e68?blobs=true).
Unauthenticated HEAD requests returned HTTP 200 and matching lengths for these three files.
No weight bytes were transferred, so the hashes were not independently recomputed.

The actual [checkpoint loader](https://github.com/jaredpalmer/kev/blob/1f04cfa28e6a708485b28a9585010b9e436b776e/kev/checkpoint.py)
fetches JSON, safetensors, PT, TXT, and Jinja files, reads the checkpoint's base reference,
then loads that pinned base and merges the adapter. Those matching files total **65,521,423
bytes for Kev plus 1,769,897,109 for Qwen**, approximately **1.835 GB combined**, excluding
Python packages and caches. Both model repositories declare Apache-2.0; the runtime's
[project metadata](https://github.com/jaredpalmer/kev/blob/1f04cfa28e6a708485b28a9585010b9e436b776e/pyproject.toml)
also declares Apache-2.0.

## Platform, memory, and this host

The author reports 3.8 GB resident GPU memory on an NVIDIA L4. On an Apple M5 with 32 GB,
an 8,192-token test used 1.5 GB of weights, 2.7 GB peak MLX memory, and a 5.3 GB process;
new-state inference took 1.4 seconds versus 74 ms with a cached state. These are vendor
measurements, not local results or game latency. Validated context is 8,192 tokens; a larger
serving limit does not establish accuracy. [Model card](https://huggingface.co/jaredpalmer/kev-0.8b)

The runtime has CPU, CUDA, and MPS device selection. Apple Silicon uses MLX for this hybrid
Qwen backbone when selected. This is implemented in
[serve.py](https://github.com/jaredpalmer/kev/blob/1f04cfa28e6a708485b28a9585010b9e436b776e/kev/serve.py),
[checkpoint.py](https://github.com/jaredpalmer/kev/blob/1f04cfa28e6a708485b28a9585010b9e436b776e/kev/checkpoint.py),
and [mlx_model.py](https://github.com/jaredpalmer/kev/blob/1f04cfa28e6a708485b28a9585010b9e436b776e/kev/mlx_model.py).
CPU execution exists; its performance here is unmeasured. No Windows or AMD acceleration
claim follows from the measured NVIDIA and Apple paths.

Read-only host inspection found **Apple M5, arm64, 32 GiB unified memory, macOS 27.0**.
`/Users/quzzar/.local/bin/uv python list --only-installed` confirms **Python 3.13.12** is
installed. Its unmodified interpreter lacks Torch, MLX, mlx_lm, Transformers, typesafe_sdk,
and Uvicorn. These are setup requirements, not evidence that this Mac cannot execute Kev.
Inference: the hardware matches the author's measured platform, but successful installation,
model loading, accuracy, and concurrency with Minecraft remain unverified.

## Dependencies and deferred invocation

The pinned `pyproject.toml` requires Python **>=3.12,<3.14**. Core dependencies include
Torch >=2.6,<2.9, Transformers >=5.17,<6, PEFT >=0.21, Accelerate, NumPy, datasets,
Pydantic, and scikit-learn. The `serve` extra adds FastAPI, Uvicorn, TypeSafe SDK >=0.6.0,
and **mlx-lm >=0.31.3,<0.32 on Darwin arm64**. The repository selects Python 3.13 and
provides a lockfile. See the pinned project metadata above.

After the existing GGUF experiments release the GPU, clone the runtime into an isolated
directory, check out the pinned runtime commit, and prepare that directory with:

```sh
/Users/quzzar/.local/bin/uv sync --python 3.13 --frozen --extra serve
```

The explicit MLX launch, deferred and not executed here, is:

```sh
KEV_BACKEND=mlx KEV_DTYPE=bf16 /Users/quzzar/.local/bin/uv run --frozen --extra serve python -m kev.serve --device mps --host 127.0.0.1 --port 8008 --run jaredpalmer/kev-0.8b@bf75a6a8848ea6960ff2ed108d9ed44c2941174f
```

Using an explicit backend avoids silently measuring a different execution path. Confirm
`GET /v1/models` reports the requested checkpoint, `backend=mlx`, and expected precision.
The server's actual endpoint is `POST /v1/systemone`, not `/v1/chat/completions`.

## Feeding typed actions

The actual [API code](https://github.com/jaredpalmer/kev/blob/1f04cfa28e6a708485b28a9585010b9e436b776e/kev/api.py)
accepts named `choice`, `noul`, and `score` questions. A choice accepts 1 to 255 supplied
keys and returns the winning key plus probabilities and confidence. It never executes a tool.
Objects in `state` are rendered as labelled text; this is not a native JSON world simulator.

Minimal SDK fixture, using the SDK included by the serve extra:

```python
from typesafe_sdk import Choice, TypeSafeClient

client = TypeSafeClient(
    api_key="local", base_url="http://127.0.0.1:8008", model="kev-latest"
)
answer = client.system_one(
    state={"request": "Wait here while I check the path."},
    questions={
        "action": Choice(
            instructions="Choose the requested offered action, or none if absent.",
            criteria={"follow": "Follow the requester", "wait": "Wait here", "none": "No action"},
        )
    },
)
print(answer.choices["action"].choice)
```

This demonstrates the request shape only; it has not been run and does not establish useful
action-routing accuracy. The official [TypeSafe SDK docs](https://docs.typesafe.ai/sdk) also
allow direct HTTP, so a Java adapter does not require a Python SDK in the Minecraft process.

Kithkyn's currently implemented verbs are `follow`, `wait`, `go_to_point`, and `resume` in
[ResidentActionState](../../src/main/java/com/quzzar/kithkyn/agency/ResidentActionState.java).
Build criteria from the actual offered set and retain the captured offer revision, requester,
dimension, reference, and expiry outside the model. Map a returned key to that same offer and
revalidate on the game thread. `none` is an explicit no-action decision in the fixture, not a
new executable verb. Do not fabricate destinations, targets, free-text arguments, or reasons.
The existing conversation boundary also requires dialogue; a Kev response cannot simply
replace its `{say, action}` payload.

## What remains to evaluate

1. Prepare and load the exact runtime and checkpoints after the GPU is free. Record actual
   backend, dependency lock, startup time, download hashes, and peak process memory.
2. Replay a small labelled set of Kithkyn offers, including no action, stale references,
   ambiguous instructions, and reordered candidates. Record accuracy, selection changes,
   confidence on wrong answers, and new-state versus cached latency.
3. Keep any experiment outside production action execution. Compare with Kev-4B if the
   smallest checkpoint fails; the author's routing limitation is a reason to expect failure,
   not a substitute for a game-specific result.

No dependencies were installed, models downloaded, server launched, paid API called, or GPU
work scheduled for this research. Local loading, SDK round trip, and Kithkyn behavior remain
unevaluated.
