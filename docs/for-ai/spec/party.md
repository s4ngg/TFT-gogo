<spec domain="party">

<purpose>
Community party recruitment and later party chat entry point.
Page: Party (/party).
</purpose>

<api>
<backend>
- GET /api/community/parties
  -> List&lt;PartyPostResponse&gt;
  -> Public read with optional authentication. Optional query params: mode, query.
  -> mode accepts RANKED_TFT, NORMAL_TFT, CUSTOM, or Korean labels.
  -> If the token is missing or invalid, joined is returned as false instead of failing the public read.

- POST /api/community/parties
  -> PartyPostCreateRequest, PartyPostResponse
  -> Authenticated. Creates a party recruitment post.
  -> The author is counted in party_posts.current_members but no party_applications row is created for the author.
  -> The response includes chatRoomId=party-recruitment. MVP party activity uses the fixed party recruitment chat channel instead of creating a party-specific chat tab.

- POST /api/community/parties/{partyPostId}/join
  -> PartyPostResponse
  -> Authenticated. Joins the party immediately unless it is full.
  -> Saves party_applications.status as ACCEPTED.
  -> Repeated joins are idempotent.

- DELETE /api/community/parties/{partyPostId}/join
  -> PartyPostResponse
  -> Authenticated. Cancels the current user's accepted participation.
  -> The owner cannot leave through this endpoint.
</backend>
<frontend>
- frontend/src/pages/Party/Party.tsx
- frontend/src/pages/Party/partyFilters.ts
- API layer: frontend/src/api/partyApi.ts
- Chat API layer: frontend/src/api/chatApi.ts
- Server-state hook: frontend/src/pages/Party/hooks/usePartyPosts.ts
- Realtime chat hook: frontend/src/pages/Party/hooks/useRealtimeChat.ts
</frontend>
</api>

<database>
- party_posts follows the shared ERD snapshot:
  user_id, title, content, max_members, current_members, deadline, is_closed, created_at, updated_at, game_mode, deleted_at.
- party_applications follows the shared ERD snapshot:
  party_post_id, user_id, status(PENDING/ACCEPTED/REJECTED), created_at, message, responded_at.
- party_applications uses a unique key on (party_post_id, user_id) to prevent duplicate joins by the same user.
- chat_rooms.party_post_id is reserved for the later PARTY chat slice.
- The ERD snapshot does not include a party tag column. Custom user tags are stored in a small helper table, party_post_tags, keyed by party_post_id.
</database>

<dto-contract>
<request name="PartyPostCreateRequest">
- title: string, required, max 200
- content: string, required. This maps to the current Party.tsx description text.
- gameMode: RANKED_TFT | NORMAL_TFT | CUSTOM, required
- maxMembers: number, required, 2-8
- deadline: LocalDateTime, optional
- tags: custom string array, optional, max 4 items, max 30 chars each
</request>
<response name="PartyPostResponse">
- id: party post id
- userId: author id
- title: post title
- content: post body
- gameMode: enum code for filtering, e.g. RANKED_TFT
- mode: Korean display label, e.g. 랭크
- currentMembers / maxMembers / capacity: member count display
- closed: boolean used for button disabled state
- status: Korean display label only, 모집중 or 마감
- tags: custom user tags
- joined: true when the authenticated user is the author or has an ACCEPTED application
- chatRoomId: fixed party recruitment chat room id, party-recruitment
- deadline / createdAt: date-time fields
</response>
<request name="ChatMessageCreateRequest">
- roomId: string, required. MVP allows only fixed community channel ids: general, deck-guide, party-recruitment, question-answer.
- content: string, required, max 500.
- senderName / tier: not accepted from the client. The backend derives senderName from the authenticated user and uses a server-controlled tier value until a trusted rank source is connected.
</request>
<response name="ChatMessageResponse">
- id / roomId / content / createdAt: message metadata and body
- senderName: server-derived member nickname
- tier: server-controlled display tier. MVP default is Unranked.
</response>
</dto-contract>

<business-rules>
- Game modes for new data are RANKED_TFT, NORMAL_TFT, and CUSTOM.
- Public users can read party posts.
- Creating, joining, and canceling participation require JWT authentication.
- The frontend must block unauthenticated create/join/cancel attempts before calling mutations and show a login-required state.
- Unauthenticated create/join/cancel attempts must not create optimistic local posts, joined state, chat previews, success messages, or query invalidation.
- If creating a recruitment post fails, the frontend keeps the user's draft input so the user can retry after fixing the problem.
- The authenticated JWT principal is the numeric userId set by JwtAuthenticationFilter.
- The post creator is automatically counted as the first current member.
- A party with current_members >= max_members is closed and cannot accept new participants.
- A party whose deadline has passed is closed and cannot accept new participants.
- A party whose deadline has passed must be returned as closed even if the stored is_closed value has not been refreshed yet.
- Joining through the UI button is immediate participation, not approval waiting.
- Repeated join requests by the same user return the current party state without increasing the member count again.
- The owner is returned with joined=true because the owner is counted as the first member.
- The owner cannot cancel participation through the join-cancel endpoint. Frontend must treat post.userId === auth.user.id as an owner state, not as a normal joined toggle.
- A separate close/delete policy should be defined later.
- Users can enter custom tags. Tags are limited to four items, 30 characters each.
- Creating a recruitment post returns the fixed party-recruitment chatRoomId. Party-specific chat rooms and extra user-created chat tabs are out of scope for the MVP.
- Normal users cannot create community chat channels by sending arbitrary roomId values.
- Recent chat message reads are public for dashboard previews. Sending chat messages requires authentication.
- The MVP chat API does not yet enforce party membership for room access; membership validation and chat_rooms.type = PARTY persistence are later slices.
</business-rules>

<validation>
- Backend response envelope must use ApiResponse&lt;T&gt;.
- Controller return type must be ResponseEntity&lt;ApiResponse&lt;T&gt;&gt;.
- Swagger annotations belong in CommunityPartyControllerDocs, not directly in CommunityPartyController.
- Service implementation must live in service/impl/.
- Service tests use Mockito and must not connect to a real DB.
- Frontend should use TanStack Query when Party.tsx is connected to this API. Do not store party server data in Zustand.
</validation>

<open-issues>
- Realtime chat transport uses SSE with snapshot/message events in the MVP.
- SSE reconnect/backoff policy beyond disconnected fallback should be refined in a later slice.
- Party close/delete policy for owners is still undecided.
- The party_post_tags helper table is required for custom tags because tags are not present in the shared ERD snapshot.
</open-issues>

</spec>
