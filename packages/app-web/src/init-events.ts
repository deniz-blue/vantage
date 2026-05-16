import { useATProtoAuthStore } from "./lib/atproto/useATProtoStore";

useATProtoAuthStore.getState().initialize();

// TODO
// createJetstream({
// 	onUpdate: async (source, record, event) => {
// 	},
// });
