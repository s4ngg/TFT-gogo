export const communityChatMessagesQueryKey = (roomId: string) =>
  ['community', 'chat', 'messages', roomId] as const
