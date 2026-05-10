type EventSource = string;

// Key `event-app:userdata`
interface LocalStorageUserData {
	state: {
		layers: Record<string, {
			data: {
				events: EventSource[];
			};
		}>;
	};
	version: 2;
}

export const _migrate26may_ = () => {

};
