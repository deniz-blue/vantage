import { Platform } from "react-native";
import "temporal-polyfill-lite";

if (Platform.OS === "web") {
	import("./init-web");
} else {
	import("./init-native");
}
