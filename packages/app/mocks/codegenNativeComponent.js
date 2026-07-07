import { View } from "react-native-web";
console.error("codegenNativeComponent: returning mock");
export default function codegenNativeComponent() {
	console.error("codegenNativeComponent: MOCK CALL", arguments);
	return View;
}
