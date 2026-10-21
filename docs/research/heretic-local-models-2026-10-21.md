# Heretic and abliterated variants of Kithkyn's local models

Reviewed 2026-10-21 against the model publishers' Hugging Face cards, reproduction records,
Hub file metadata, and the original model licenses. The initial research did not download or
run weights. Follow-up local evaluation downloaded and SHA-256-verified the Llama Heretic file;
its packaging findings below supersede the metadata-only compatibility inference.

## Answer

**Llama-3.2-3B-Instruct has verified Heretic variants, including an already downloadable
Q4_K_M GGUF. Gemma-2-2B-it has a verified abliterated variant and Q4_K_M GGUF, but I did not
verify an exact-base Gemma-2-2B-it checkpoint made with Heretic.** Those are different claims.

Heretic is an automated implementation of directional abliteration with an optimizer that
balances refusal reduction against divergence from the original model. A model described as
abliterated or uncensored need not have used Heretic. See the
[Heretic project](https://github.com/p-e-w/heretic).

Kithkyn currently recommends `Llama-3.2-3B-Instruct` and `Gemma-2-2B-it`, both Q4_K_M, and
pins llama.cpp `b10653`. These are recorded in
[LlamaServerLauncher](../../src/main/java/com/quzzar/kithkyn/llm/LlamaServerLauncher.java).
Gemma 4 derivatives are a different model generation and do not answer the Gemma 2 question.

## Candidates and provenance

| Candidate | Verified origin and method | Availability |
| --- | --- | --- |
| [saidutta69/Llama-3.2-3B-Instruct-heretic](https://huggingface.co/saidutta69/Llama-3.2-3B-Instruct-heretic) | Publisher documents `meta-llama/Llama-3.2-3B-Instruct`, edited with Heretic 1.4.0. Includes a reproduction record, selected parameters, and original weight hashes. | Safetensors and its own Q4_K_M GGUF. This is the simplest verified Heretic candidate to evaluate. |
| [richardyoung/Llama-3.2-3B-Instruct-heretic](https://huggingface.co/richardyoung/Llama-3.2-3B-Instruct-heretic) | A separate Heretic 1.4.0 run of that exact base, with different parameters and resulting weight hashes. | Safetensors and reproduction records; no GGUF listed in the reviewed repository. |
| [IlyaGusev/gemma-2-2b-it-abliterated](https://huggingface.co/IlyaGusev/gemma-2-2b-it-abliterated) | Author explicitly identifies `google/gemma-2-2b-it` and directional abliteration. The card identifies the selected activation candidate, rather than Heretic. | Safetensors, with [bartowski's GGUF conversion](https://huggingface.co/bartowski/gemma-2-2b-it-abliterated-GGUF) explicitly naming this checkpoint as its source. |

Both Llama reproduction records pin the original Meta revision
`0cb88a4f764b7a12671c53f0838cd831a0843b95`. Their independent runs and metrics are recorded in
[saidutta69's reproduction JSON](https://huggingface.co/saidutta69/Llama-3.2-3B-Instruct-heretic/blob/226645d6f970ac002e72fab956c41401e1a0768a/reproduce/reproduce.json)
and [richardyoung's reproduction JSON](https://huggingface.co/richardyoung/Llama-3.2-3B-Instruct-heretic/blob/338036ce1c2ca666150ef85c22ea4e40ba2fd12c/reproduce/reproduce.json).

One misleadingly similar result is
[mradermacher/Llama-3.2-3B-Instruct-heretic-GGUF](https://huggingface.co/mradermacher/Llama-3.2-3B-Instruct-heretic-GGUF).
Its own metadata names `rawcell/Llama-3.2-3B-Instruct-heretic` as the source, not either of
the runs above. Do not attach saidutta69's or richardyoung's measurements to that file.

The exact-base Gemma search checked public Hub searches for `gemma-2-2b-it-heretic` with the
`gemma2` architecture filter and inspected the matching family cards. It did not establish a
direct Heretic checkpoint. This is a search result, not proof that none exists. The verified
IlyaGusev variant is an alternative abliteration route. A Japanese Swallow derivative, a
merge, or a Gemma 4 model would introduce additional changes beyond refusal removal.
See the [Hub query](https://huggingface.co/api/models?search=gemma-2-2b-it-heretic&filter=gemma2&limit=1000&full=true).

## Downloadable artifacts to pin for evaluation

Public, unauthenticated HEAD requests to both pinned download URLs returned HTTP 200 with
the exact byte lengths below. This verifies availability without transferring the weights.
Sizes and hashes are publisher-hosted LFS metadata, not locally computed checksums.

| Field | Llama Heretic | Gemma 2 abliterated |
| --- | --- | --- |
| Repository | `saidutta69/Llama-3.2-3B-Instruct-heretic` | `bartowski/gemma-2-2b-it-abliterated-GGUF` |
| Revision | `226645d6f970ac002e72fab956c41401e1a0768a` | `11124c8787de96de70c3d5cb8d5d49c420ebcdf8` |
| File | `Llama-3.2-3B-Instruct-heretic-Q4_K_M.gguf` | `gemma-2-2b-it-abliterated-Q4_K_M.gguf` |
| Bytes | 2,019,373,408 | 1,708,582,784 |
| Decimal GB | 2.019 | 1.709 |
| SHA-256 | `e780f98b35c991bc1d56331d7fac3b07d8a83356241962d13e5fdae8ff81e9a0` | `5f0264dd8197b8f96b6f363461cc9d5b7ad49284d2849c68942f43109a1252c4` |

Sources: [Llama Hub metadata](https://huggingface.co/api/models/saidutta69/Llama-3.2-3B-Instruct-heretic?blobs=true),
[Gemma Hub metadata](https://huggingface.co/api/models/bartowski/gemma-2-2b-it-abliterated-GGUF?blobs=true).
The original Gemma abliterated repository was reviewed at
`b7f67f3bcbbcbf275f158bff124cb7f0c448e9d6`; the converter identifies the repository but does
not pin its exact source revision in the card. That provenance limitation remains.

Pinned downloads:

- [Llama Heretic Q4_K_M](https://huggingface.co/saidutta69/Llama-3.2-3B-Instruct-heretic/resolve/226645d6f970ac002e72fab956c41401e1a0768a/Llama-3.2-3B-Instruct-heretic-Q4_K_M.gguf)
- [Gemma 2 abliterated Q4_K_M](https://huggingface.co/bartowski/gemma-2-2b-it-abliterated-GGUF/resolve/11124c8787de96de70c3d5cb8d5d49c420ebcdf8/gemma-2-2b-it-abliterated-Q4_K_M.gguf)

## Compatibility and memory

The Hub GGUF metadata reports standard `llama` and `gemma2` architectures respectively. Both
use the same architecture and quantization families
as Kithkyn's existing choices, and their file sizes are essentially unchanged. The Gemma
conversion card records llama.cpp `b3496` for quantization. This supports compatibility in
principle, but does not establish correct packaging or successful inference.
See the Hub metadata above and [Gemma conversion card](https://huggingface.co/bartowski/gemma-2-2b-it-abliterated-GGUF).

The verified Llama Heretic GGUF has **no `tokenizer.chat_template` entry**, while the stock
Llama GGUF does. Out of the box, llama.cpp b10653 selected ChatML; the first Kithkyn action
request took 62.6 seconds including a retry and returned several fake assistant turns with
ChatML delimiters. That is an artifact/runtime configuration failure, not a clean model-quality
comparison. Do not recommend this file with the unmodified launcher. The comparison therefore
configures the Llama pair explicitly with `--no-jinja --chat-template llama3` and checks the
rendered template before scoring.

With b10653's default Jinja engine, `--chat-template llama3` alone was treated as literal text;
the runtime's `/apply-template` response was exactly `llama3`, omitting the messages. This failed
test configuration was discarded. Always verify a template override against actual rendered
system/user text rather than inferring its meaning from a flag name.

Gemma's native template requires alternating user/assistant turns and treats system roles
differently from Llama. The existing Java provider sends a system message; it has no dedicated
Gemma adaptation. The comparison explicitly selects the runtime's `gemma` format and checks
that it includes both system instructions and user text. Do not assume a weight edit changes
the supported message format. Disk size is not total runtime memory: context and inference
buffers remain additional.

Inference: refusal edits do not inherently require a larger model or a second resident model.
These candidates should occupy the same broad memory class as the originals, but generated
length, latency, and conversation quality need measurement.

## Published quality evidence and gaps

The Llama publishers report these metrics for their edited checkpoints:

| Metric | saidutta69 | richardyoung | Original |
| --- | ---: | ---: | ---: |
| Refusals on 100 test prompts | 2 | 2 | 97 |
| KL divergence | 0.0327 | 0.0625 | 0 by definition |

The reproduction JSON identifies `mlabonne/harmful_behaviors` test prompts for refusal
evaluation and `mlabonne/harmless_alpaca` test prompts for the general-output comparison.
These are author-reported results on those sets. They do not measure Minecraft decisions,
JSON adherence, dialogue variety, persona consistency, memory, or game performance. Low KL
does not establish unchanged ability. The Gemma 2 author's card supplies examples but no
corresponding quantitative Kithkyn or general-capability comparison.

The useful next evaluation is a paired replay against the current models: the same legitimate
in-character requests, multi-turn conversations, action payloads, and bounded village choices.
Measure refusals that obstruct gameplay, usable decisions, invalid JSON/actions, repetition,
persona drift, latency, and memory. Changing refusal behavior does not add missing game tools
or make a model's chosen action executable.

## Licenses

The Llama candidates declare the Llama 3.2 Community License. Its redistribution terms retain
the agreement, attribution notice, applicable use policy, and the required Llama identification.
The edited model is not relicensed under Heretic's code license.
See [Meta's license](https://github.com/meta-llama/llama-models/blob/main/models/llama3_2/LICENSE).

The Gemma 2 checkpoint and conversion declare the Gemma license. Google's Gemma Terms apply
to modifications and derivatives regardless of the download source, with redistribution
notices and use restrictions. Gemma 4's different licensing does not apply to Gemma 2.
See [Gemma Terms](https://ai.google.dev/gemma/terms). These are reported source terms, not a
legal determination about a proposed distribution.

No recommended defaults, production model selector, pinned runtime, or release configuration
were changed by this research or the experimental replay harness.
