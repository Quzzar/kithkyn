# Halo companion model stack

Current scope, 2026-10-21: player companion commands and their status UI were removed.
The interaction is the existing right-click conversation. Any companion-command prompts,
counts or suggestions below are historical evidence, not current implemented scope.
The live evaluation corpus and parser no longer offer or score those commands.

Reviewed 2026-10-21 against the model authors' cards, runtime repositories, and TypeSafe's API
documentation. This is research, not a model benchmark or an implementation decision.

## Scope and evidence

The original YouTube page identifies the video as
[Giving Video Game NPC's Consciousness](https://www.youtube.com/watch?v=EOfm3h4xYU8),
by chasm (@chasmmm), published 2026-10-16. Its expanded description directly links Kev-4B,
Gemma 4 12B, Parakeet TDT 0.6B v2, Breeze TTS 2, and OpenCE. This provenance was checked
against the original page, not solely a search summary.

The original auto-generated transcript was also checked. It confirms the creator's description
of the layers; it is not source code or a reliability benchmark. The description resolves the
caption's misrecognition of Parakeet. The cards below establish what each linked model can do.

## What the creator explains

- At [1:56](https://www.youtube.com/watch?v=EOfm3h4xYU8&t=116s), he attributes the quick jump
  response to a fast layer that acts before the generative LLM.
- At [2:52](https://www.youtube.com/watch?v=EOfm3h4xYU8&t=172s), he names Kev as the
  classifier, Gemma 4 12B for thinking, Breeze TTS 2 for speech, and Parakeet for transcription.
  He says these models run locally.
- At [7:02](https://www.youtube.com/watch?v=EOfm3h4xYU8&t=422s), he explains that speech
  and game events trigger the fast layer, which decides whether more thinking is needed.
- At [8:00](https://www.youtube.com/watch?v=EOfm3h4xYU8&t=480s), he says in-game voice
  lines also enter the companion's context.

At [13:50](https://www.youtube.com/watch?v=EOfm3h4xYU8&t=830s), he describes the project as a
proof of concept. No measured reliability or complete latency distribution is supplied.

## The important distinction

**Kev chooses among supplied options. A separate language model writes the dialogue, and a
speech model turns that dialogue into audio.** TypeSafe's Jev API evaluates named questions
against a shared `state`; its response contains typed answers and probabilities. It does not
constitute a speech pipeline. See [System One](https://docs.typesafe.ai/api/system-one).

Kev is an independently available family of local, Jev-style decision models with a compatible
System One API. It supports yes/no, choice, and score questions. The project includes 0.8B,
4B, 9B, and 27B variants and CUDA and Apple Silicon MLX serving paths. These are separate Kev
checkpoints, not downloadable Jev weights. See the [Kev repository](https://github.com/jaredpalmer/kev).

| Component | Verified role | What this establishes |
| --- | --- | --- |
| [Kev-4B](https://huggingface.co/jaredpalmer/kev-4b) | Qwen3.5-4B-Base with a LoRA adapter and pointer head; scores supplied options in a forward pass without generating text. | Suitable for testing bounded choices. Its card explicitly excludes chat and open-ended generation. |
| [Gemma 4 12B](https://huggingface.co/google/gemma-4-12B) | A text-generating model accepting text, image, video, and audio input, with native function calling. | Can write dialogue and interpret context. Audio input does not mean that it produces the spoken voice. |
| [Parakeet TDT 0.6B v2](https://huggingface.co/nvidia/parakeet-tdt-0.6b-v2) | NVIDIA's 600M-parameter English speech recognition model, taking mono 16 kHz audio and returning text, optionally with timestamps. | Transcribes the player's microphone input; does not write character replies. |
| [Breeze TTS 2](https://huggingface.co/BreezeBlue/Breeze-TTS-2) | Generates English and Chinese speech, with voice design, reference-based voice cloning, delivery instructions, and vocal events. | Can supply expressive spoken delivery after another component provides the utterance. |

A possible separation of responsibilities is therefore:

1. Transcribe microphone speech.
2. Evaluate relevant events and commands as bounded action choices.
3. Generate character dialogue when a reply is useful.
4. Synthesize and play that dialogue.

This follows the described layers, but is not a verified reconstruction of the video's code.
In particular, a decision model selects values; application code still validates
and executes the resulting game action. TypeSafe demonstrates that division in its
[function-calling cookbook](https://docs.typesafe.ai/cookbooks/function_calling).

## Local hardware and platform constraints

The advertised setup is materially heavier than a single small quantized chat model.

- **Kev-4B:** the published serving path uses BF16. Its card reports 14.3 GB resident GPU
  memory and recommends about 16 GB free GPU memory. The reported 8,192-token Apple M5 test
  used an 11.9 GB process footprint; the card recommends a 32 GB Apple Silicon Mac. Those
  are the author's measurements, not Kithkyn results. Its validated state length is 8,192
  tokens despite the larger serving limit. [Model card](https://huggingface.co/jaredpalmer/kev-4b)
- **Smaller decision option:** the Kev repository lists Kev-0.8B for a 4 GB GPU or any Apple
  Silicon Mac. It reports lower decision accuracy than larger checkpoints. This makes it a
  candidate for a footprint-first experiment, not evidence of good village decisions.
  [Kev model comparison](https://github.com/jaredpalmer/kev#models)
- **Gemma 4 12B:** Google's published BF16 `model.safetensors` is 23.9 GB. Working memory and
  context cache are additional. A quantized runtime could reduce the weight footprint, but
  the demonstration's quantization and actual resident memory are unverified.
  [Official files](https://huggingface.co/google/gemma-4-12B/tree/main)
- **Parakeet:** NVIDIA documents NeMo/PyTorch use and GPU-optimized execution. Its card's
  large-batch throughput result is not a measurement of one player's live microphone
  latency. A cross-platform microphone service needs its own measurements.
  [Model card](https://huggingface.co/nvidia/parakeet-tdt-0.6b-v2)
- **Breeze:** the official runtime requires Linux, Python 3.10+, and an NVIDIA CUDA GPU.
  It reports about 7.7 GiB for single-request eager inference, recommending a 12 GB GPU, or
  14.4 GiB and a 24 GB GPU for its fast path. Its under-40 ms first-audio claim is for a
  warmed H100, not a typical gaming PC. The streaming API emits mono 24 kHz signed 16-bit
  PCM and handles one request concurrently. [Official runtime](https://github.com/breezeblue-ai/breeze-tts)

Inference: running every component beside Minecraft on one GPU could create substantial
memory and frame-time contention. Summing vendor weight sizes does not establish a total
requirement because residency, offloading, quantization, context, and scheduling differ.
Measure the whole pipeline, including silence detection, transcription, decisions, dialogue,
first playable audio, and game frame times.

## Distribution constraint for the voice model

Breeze's source code is Apache-2.0, but its weights and self-hosted outputs use the
BreezeBlue Research and Non-Commercial License v1.1. The license requires a separate written
commercial license for commercial use, including product, application, or plugin deployments.
The public code license therefore does not establish permission to bundle this voice stack
in a shipped product. This is a reported license condition, not a legal assessment of Kithkyn.
See the [model license](https://huggingface.co/BreezeBlue/Breeze-TTS-2/blob/main/LICENSE).

## What still needs verification

- Determine the actual tool schemas, event triggers, memory format, dialogue scheduling,
  voice settings, model variants, quantizations, and hardware. These cannot be recovered
  from the model cards.
- Confirm whether any integration code is public. The linked
  [OpenCE repository](https://github.com/OpenCommunityEdition/OpenCE) is the Halo game port;
  its README alone does not establish that it includes chasm's companion integration.
- Compare any proposed Kithkyn change with the existing separation between game rules,
  bounded model choices, generated conversations, and game action execution. Existing
  [Jev fit research](jev-kithkyn-fit-2026-09-23.md) already documents why a decision model
  cannot replace the shared generative provider wholesale; its service facts are dated and
  should be rechecked before reuse.

## What applies to Kithkyn

This is a proposed experiment, not an approved implementation or a change to the default model.

Kithkyn already has a shared conversation turn, resident memories, structured reply effects,
and game-state briefings. Player chat and autonomous resident chat share
[`PersonChatDispatcher.converse`](../../src/main/java/com/quzzar/kithkyn/chat/PersonChatDispatcher.java),
and nearby ambient lines already travel through
[`VillagerSpeechPacket`](../../src/main/java/com/quzzar/kithkyn/networking/VillagerSpeechPacket.java)
to text bubbles. The integration opportunity is to connect speech input/output and timely
world reactions to those existing character systems. See [conversations](../conversations.md)
and [villager conversations](../villager-conversations.md).

A useful first demonstration would focus on one villager: push-to-talk input, one consistent
voice, short replies, and a few grounded reactions to actual nearby events. Feed engine-owned
locations, work, relationships, and recent events into the context. Test ordinary conversation
alongside an existing validated item handoff or village request, so speech has an observable
connection to the world. Coordinate audio with subtitles, distance and speaker identity;
queueing and interruption behavior matter as much as the voice sample.

Evaluate a separate Kev decision experiment against existing logged choices. Kev's compatible
System One endpoint is not compatible with Kithkyn's text-generation contract: current
[`LlmService.decide`](../../src/main/java/com/quzzar/kithkyn/llm/LlmService.java) expects an
authored reason as well as a chosen action. Decide deliberately which call sites can use
probabilities alone. Kev-0.8B is a smaller candidate worth comparing, but its village accuracy
and latency are unmeasured. No decision model should run on every movement tick merely to
copy the demonstration's terminology.

Before selecting a voice engine for distribution, establish the actual permission and platform
requirements. Breeze's published model license does not grant unrestricted product deployment,
and its documented CUDA runtime would substantially narrow a zero-setup local feature. These
constraints favor testing the interaction first and making any shipped voice configuration an
explicit product decision.

No models were downloaded or run for this research. No code, runtime, or product settings
were changed.
