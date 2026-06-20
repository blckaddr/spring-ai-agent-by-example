# Chapter 6 — Giving It Memory *(Phase 3)*

## What we wanted to learn

Every request so far was an island. You could ask the agent to convert 100 USD to GBP, but you
couldn't then say *"now convert that to yen"* — there was no "that." Each call started from a
blank slate. This phase turns the agent from a **stateless function** into something that
**remembers a conversation**: a follow-up can refer back to what came before.

## What we built

Three small pieces, and one principle.

**A memory store.** A `ChatMemory` that keeps a sliding window of recent messages, held in memory
and addressed by a **conversation id**:

```java
MessageWindowChatMemory.builder()
    .chatMemoryRepository(new InMemoryChatMemoryRepository())
    .maxMessages(20)
    .build();
```

**A memory advisor.** Spring AI's `MessageChatMemoryAdvisor`, registered as a default advisor on
the ChatClient. An "advisor" wraps each model call; this one does two things automatically:
before the call it **loads** the conversation's prior messages and prepends them, and after the
call it **saves** the new exchange. We never manually stitch history into the prompt.

**A session id on the request.** The endpoint now accepts a `sessionId`, which we pass as the
conversation id for that call:

```java
.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
```

**The principle: state lives *outside* the request.** The conversation isn't held in a controller
field or a static variable — it lives in the `ChatMemory` store, keyed by id. That's what lets two
users (or two browser tabs) have independent conversations against the same agent, and it's why
this scales beyond a toy. (For learning the store is in-memory; production would put it in Redis or
a database so it survives restarts and is shared across instances.)

## What actually happened

We ran two turns in the same session (`demo-1`):

**Turn 1 —** *"Add 100 USD and 50 EUR together and give the total in GBP."*

```
convert(EUR→GBP)=42.93 ; convert(EUR→GBP)=42.93 ; add([42.93, 42.93]) = 85.86
ANSWER: ...the total ... is 85.86 GBP.
```

**Turn 2 —** *"Now convert that total to Japanese yen."*

```
convert(85.86, GBP→JPY) = 17063.32
ANSWER: ...converted to Japanese yen, is 17063.32 JPY.
```

Look closely at turn 2. The input never mentions **85.86** — yet the model called
`convert(85.86, GBP, JPY)`. It pulled that number out of the *previous* turn, which the memory
advisor had quietly reloaded. "That total" resolved to a value the user never repeated. The agent
remembered.

To prove the memory is genuinely **keyed per session** and not just global state, we sent the
exact same follow-up in a *different* session (`other`) with no history:

```
steps: 0
ANSWER: To proceed, I need the total amount in its current currency... Could you please
        provide me with the specific amount and its original currency?
```

No context, no resolution — it asked for the number. Same question, different session, opposite
behavior. Memory is per-conversation, exactly as intended.

### An honest wrinkle: memory ≠ correctness

Turn 1 is actually *wrong*. The task was 100 USD + 50 EUR, but the model converted the EUR twice
and dropped the USD entirely, producing 85.86 GBP instead of the correct 121.93. Even the 14B
model occasionally fumbles the orchestration (the Chapter 3 demon, not fully exorcised).

What's instructive is what memory did with that mistake: turn 2 faithfully carried the **wrong**
85.86 forward and converted *it*. Memory doesn't validate; it remembers. It will propagate a bad
number as happily as a good one. A stateful agent compounds its own errors across turns — which
makes the per-step visibility from Phase 0 matter *more*, not less, once memory is involved.

## What it taught us

- **Conversation is just state with an address.** "Memory" sounds mystical; mechanically it's a
  list of past messages, stored by conversation id, reloaded before each call. An advisor makes it
  automatic, but it's not magic.
- **Keying is the whole game.** The conversation id is what separates users and turns it from a
  global blob into real multi-tenant memory. Get the key wrong and everyone shares one brain.
- **State belongs outside the request.** Putting it in the store (not in the bean) is what makes
  concurrent, independent conversations possible — and is the seam where you'd later swap in Redis
  without touching the agent logic.
- **Memory amplifies whatever you feed it — including mistakes.** A wrong earlier answer becomes a
  wrong premise for every later turn. Statefulness raises the stakes on correctness and on
  observability.

The agent can now hold a conversation. But every loop so far has run *synchronously* — the caller
waits, holding the HTTP connection, until the whole thing finishes. Real agent runs can take
minutes. Next we let the loop run detached.

→ *Chapter 7 — Letting It Run Detached (Phase 4, upcoming)*
