# Local model replay, 2026-10-21

Keep the current model recommendations. The modified checkpoints are experiments, and this
replay does not establish that either improves Kithkyn's residents. Conversational action
selection remains experimental and disabled by default.

## What ran

Four Q4_K_M files were downloaded or reused and verified against pinned sizes and SHA-256
hashes. The harness ran them serially through Kithkyn's actual `PersonChatContext`, initial
chat request builder, decision request builder and `OpenAiCompatibleProvider`. No cloud
service or paid inference was used.

Each model answered 22 isolated scenarios three times, then one 12-turn conversation:
78 responses per model, 312 total. The scenarios contain four explicit movement requests,
five cases that should select no movement, three inventory requests, two memory questions,
two roleplay exchanges and six simple bounded decisions. This is a small regression corpus,
not a measure of general intelligence or a populated village acceptance test.

The synthetic resident is Mira Flint, a friendly but independent adult wanderer with a stone
axe, three bread and four oak logs, no village and no urgent duties. Rowan is a simulated
speaker. The release case first establishes a real active follow intention; its captured
prompt confirms that state. The failed-arrival case supplies a personal memory explicitly
recording the missing bridge and failed journey. Isolated-case request hashes match across
all candidates. Conversation histories differ because they contain each model's replies.

Hardware: Apple M5, 32 GiB unified memory, macOS 27.0, arm64. The runtime is llama.cpp
`b10653`; the native executable SHA-256 is
`c32a2010dec561243448447599b7c13789022052ed59a336005758fbacf03639`.
One native model ran at a time, using 8,192 total context tokens and two slots, after a
separate warm-up. Sampling uses the production request settings; runtime seeds were not
controlled. Repeats are not guaranteed independent. Request latency includes provider work
but excludes download, runtime startup and warm-up.

| Model | Pinned revision | File size, bytes |
| --- | --- | ---: |
| [Llama 3.2 3B](https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF) | `5ab33fa94d1d04e903623ae72c95d1696f09f9e8` | 2,019,377,696 |
| [Llama 3.2 3B Heretic](https://huggingface.co/saidutta69/Llama-3.2-3B-Instruct-heretic) | `226645d6f970ac002e72fab956c41401e1a0768a` | 2,019,373,408 |
| [Gemma 2 2B](https://huggingface.co/bartowski/gemma-2-2b-it-GGUF) | `855f67caed130e1befc571b52bd181be2e858883` | 1,708,582,752 |
| [Gemma 2 2B abliterated](https://huggingface.co/bartowski/gemma-2-2b-it-abliterated-GGUF) | `11124c8787de96de70c3d5cb8d5d49c420ebcdf8` | 1,708,582,784 |

Exact filenames, checksums, requests, template probes, raw replies and timings are in
[`tools/llm/results/2026-10-21/`](../../tools/llm/results/2026-10-21/). The
[candidate research](heretic-local-models-2026-10-21.md) records provenance and license
differences. The Gemma derivative is documented as abliterated, not as made with Heretic.

## Packaging affected the result

The Llama Heretic GGUF omits `tokenizer.chat_template`. The unmodified runtime selected
ChatML and returned fake assistant turns, taking 62.6 seconds including a retry on the first
action request. That is a packaging/configuration failure, not a clean weight comparison.

The scored run therefore explicitly used `--no-jinja --chat-template llama3` for both Llama
files and `--no-jinja --chat-template gemma` for both Gemma files. `/apply-template` probes
verified that system instructions and user text survived rendering. Gemma's legacy formatter
folds the system instructions into its first user turn. The scored results do not establish
drop-in compatibility with the production launcher's default template selection.

An earlier attempt passed `--chat-template llama3` with Jinja enabled. This runtime treated
that value as literal text and omitted the messages. Those outputs were discarded and are
not in the scored evidence. Explicit format flags must be checked against rendered messages.

## Counts and timing

All 312 requests returned a response. The table separates response shape from requested
behavior; a successful generation is not necessarily a correct action.

| Model | Strict chat /60 | Requested action /12 | Strict no-action /15 | Strict correct decision /18 | Median / p95, ms | Max sampled RSS, MiB |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Llama 3.2 3B | 58 | 7 | 7 | 18 | 1,817 / 2,938 | 3,600 |
| Llama Heretic | 58 | 6 | 0 | 18 | 1,958 / 3,603 | 3,406 |
| Gemma 2 2B | 51 | 8 | 8 | 14 | 2,579 / 3,605 | 3,900 |
| Gemma abliterated | 37 | 4 | 6 | 6 | 3,026 / 4,217 | 3,790 |

| Model | Strict item replies with legal/no transfer, /9 | Invalid item claims inside strict replies | Extra actions outside action cases | Fight flags outside threat cases |
| --- | ---: | ---: | ---: | ---: |
| Llama 3.2 3B | 9 | 0 | 6 | 0 |
| Llama Heretic | 8 | 1 | 6 | 2 |
| Gemma 2 2B | 6 | 1 | 0 | 0 |
| Gemma abliterated | 2 | 2 | 0 | 0 |

Strict chat means one JSON object with nonempty string dialogue and an optional supported
action. Markdown fences and duplicate keys fail. Production dialogue can recover text from
some such replies, while the movement boundary deliberately rejects them. These counts are
not counts of conversations that would display nothing in the game.

Request matches measure the exact requested action on four cooperative scenarios. A grounded
refusal can be legitimate, so a missing match alone is not an unsafe response. The no-action
cases check hypothetical speech, another person's action, missing pointing, a greeting and
an invented tool. A wrong supported action can pass the action-name validator, which is why
that boundary alone does not prove the model understood the request.

The decision column requires strict JSON, a valid option number and matching echoed option
text, using `LlmService.parseStrictDecision`. The production generic decision parser is more
permissive. These six obvious situations do not establish planning or tactical ability.

RSS is the largest native-process sample taken after a response. It is not peak total RAM,
Metal allocation, or the combined game-plus-model memory requirement. Latency is from one
serial run on this host; it does not establish queue behavior or frame/tick performance.

## Reading the replies

The stock Llama first answered a follow request with a claim about afternoon work and
`action: wait`. The Heretic variant claimed an obligation to deliver logs and selected
`resume`, despite the fixture having no urgent work or village. Stock Gemma also sometimes
agreed to accompany Rowan while selecting `wait`. These are word/action or grounding errors,
not evidence that native navigation failed.

The Heretic variant emitted action fields in every negative action scenario. Its conversation
also returned two `action` fields on a goodbye, which the strict boundary rejects. It set
`fight: true` when asked about a fictitious shared dragon ride, and once when asked for more
bread than it had. The stock Llama and stock Gemma emitted no fight flag outside the explicit
threat cases in this run. Refusal reduction does not by itself make interactions appropriate.

All three of those models described the supplied failed journey as unsuccessful in all
three repeats. Llama and Heretic nevertheless attached a new movement selection to those
recall replies. Their conversation also invented details: stock Llama mentioned chatting
with Steve and a new village bridge; Heretic assumed Rowan had moved into a village; stock
Gemma described tending wheat and said the sun was setting during a fixed afternoon.
Dialogue-only history did not update physical inventory: stock Gemma offered bread twice.
That is not a measured duplication bug because no item transfers were executed in this replay.

The Gemma derivative also recalled the supplied failed journey in all three repeats. However,
it accepted the invented shared dragon memory in the conversation, calling it a fine beast.
It proposed the nonexistent `teleport` action and selected a transfer of 100 bread from three.
Much of its strict-format failure came from Markdown fences, and some decision replies
shortened the echoed option text. These are output-contract failures, not proof that every
rejected decision had selected the wrong underlying option.

Inventory scoring checks only whether a strict reply's selected item and quantity fit the
fixture's stock, or whether it selected no transfer. It does not prove conservation in the
game or distinguish every verbal promise from its payload. Heretic invented `crafting:bread`
in one reply. Stock Gemma's `give: null` fails this strict schema but is ignored by the current
dialogue parser. See raw replies before interpreting a failed count as an actual transfer.

All models' full memory, roleplay and 12-turn transcripts were read manually. The distinct
opening heuristic in the summary is merely a repetition indicator; no automatic judge or
persona-quality score was used. Ordinary rough dialogue did not require refusal removal in
the stock models in this corpus.

## Reproduce

Use the opt-in `runLocalModelEvaluation` Gradle profile. Its world, configuration, native
runtime, weights and original output live under `run/local-model-evaluation`, separate from
normal development saves. On first launch it may stop for Minecraft's EULA. After accepting
the EULA for that directory, configure `config/kithkyn-common.toml` there:

```toml
[general]
"Generate villages" = false

[llm]
"Enable LLM?" = false
"Experimental resident actions" = true
```

These settings prevent production inference from competing with the candidate runtime and
enable the follow fixture. Incompatible settings fail explicitly. Then run from the repo root:

```sh
./gradlew runLocalModelEvaluation --console=plain
uv run --python 3.13 python tools/llm/summarize.py \
  run/local-model-evaluation/results/llama.json \
  run/local-model-evaluation/results/heretic.json \
  run/local-model-evaluation/results/gemma.json \
  run/local-model-evaluation/results/abliterated.json \
  --output run/local-model-evaluation/results/summary.json
```

`-PevalModels=llama,heretic` selects an explicit subset; `-PevalRepeats=1` shortens isolated
case repeats. Each run replaces its selected candidates' report files. Inspect every report's
`status`, expected sample count and the summary; `RESULT COMPLETE` means the harness ended,
not that the models passed. Empty responses count as failures. The scorer rejects incomplete
reports and mismatched isolated-case prompts. Run its regression tests with
`uv run --python 3.13 python -m unittest discover -s tools/llm -p 'test_*.py'`.

To rescore the checked-in capture without inference, replace each `run/local-model-evaluation/results`
path above with `tools/llm/results/2026-10-21` and write the output outside that evidence directory.
The summary includes source-report and corpus hashes.

## Remaining work

This harness captures raw completions. It does not run the dispatcher's repeated-answer
retry, physically tick the resident, transfer items, or test multiplayer, persistent goals,
speech or populated-village performance. Request builders are shared with production; this
does not make the harness an end-to-end gameplay test. The separate native-goal fixture
tests physical execution with inference disabled.

Next, improve the agreement/action contract and grounded context, then repeat the real-model
companion scenario in [#186](https://github.com/Quzzar/kithkyn/issues/186). Keep
[#191](https://github.com/Quzzar/kithkyn/issues/191) open for broader personas, adversarial
cases, physical inventory acceptance and a separately measured Kev decision checkpoint.
The smallest public Kev checkpoint's own author advises against tool routing; the
[Kev setup research](kev-local-evaluation-2026-10-21.md) is preparation, not a local benchmark.
