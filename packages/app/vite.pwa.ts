import type { ManifestOptions } from "vite-plugin-pwa";

const icon = (size: number) => [
	{
		src: `icon${size}.png`,
		type: "image/png",
		sizes: `${size}x${size}`,
		purpose: "any",
	},
	{
		src: `icon${size}-maskable.png`,
		type: "image/png",
		sizes: `${size}x${size}`,
		purpose: "maskable",
	},
];

export const PWAManifest: Partial<ManifestOptions> = {
	id: "/",
	name: "Vantage",
	short_name: "Vantage",
	description: "View and manage events",
	categories: ["utilities", "calendar", "productivity"],
	dir: "ltr",
	lang: "en-US",
	theme_color: "#242424",
	background_color: "#242424",
	start_url: "/",
	display: "standalone",
	display_override: ["window-controls-overlay", "standalone", "fullscreen", "minimal-ui"],
	prefer_related_applications: false,
	icons: [
		{
			src: "icon.svg",
			type: "image/svg+xml",
			sizes: "any",
			purpose: "any",
		},
		...icon(192),
		...icon(256),
		...icon(512),
	],
	orientation: "any",
	protocol_handlers: [
		{
			protocol: "web+evnt",
			url: "/?protocol-handler=%s",
		}
	],
	handle_links: "preferred",
	launch_handler: {
		client_mode: "navigate-existing",
	},
	scope: "/",
	scope_extensions: [
		{ type: "origin", origin: "https://eventsl.ink" },
	],
	shortcuts: [
		{
			name: "Home",
			url: "/",
		},
		{
			name: "List",
			url: "/list",
		}
	],
	file_handlers: [
		{
			action: "/?file-handler=%s",
			accept: {
				"application/json": [".json"],
				"text/calendar": [".ics"],
			},
		},
	],
	share_target: {
		action: "/?share-target",
		method: "POST",
		enctype: "multipart/form-data",
		params: {
			url: "url",
			files: [
				{
					name: "json",
					accept: ["application/json"],
				},
				{
					name: "ics",
					accept: ["text/calendar"],
				},
			],
		},
	},
};
