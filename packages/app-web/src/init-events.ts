import { useATProtoAuthStore } from "./lib/atproto/useATProtoStore";
import { useCacheEventsStore } from "./lib/cache/useCacheEventsStore";

window.addEventListener("storage", (event) => { });
useATProtoAuthStore.getState().initialize();
(async () => {
	console.time("Cache initialization");
	await useCacheEventsStore.getState().init();
	console.timeEnd("Cache initialization");
})();

// TODO
// createJetstream({
// 	onUpdate: async (source, record, event) => {
// 	},
// });
