import "temporal-polyfill-lite";
import { App } from "expo-router/build/qualified-entry";
import { createRoot } from "react-dom/client";
import React from "react";

const rootElement = document.getElementById("root")!;
const root = createRoot(rootElement);

root.render(
	<React.StrictMode>
		<App />
	</React.StrictMode>
);
