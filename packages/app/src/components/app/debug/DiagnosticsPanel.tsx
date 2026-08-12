import { useState } from "react";
import { Box } from "../../base/Box";
import { Text } from "../../base/Text";
import { Button } from "../../base/button/Button";
import { AsyncButton } from "../../base/button/AsyncButton";
import { FontSize } from "../../../theme/sizing";
import { Colors } from "../../../theme/colors";
import {
	identityResolver,
	handleResolver,
	didDocumentResolver,
	AtOAuthClient,
} from "@vantage/atproto";

type LogEntry = { label: string; ok: boolean; detail: string };

async function check(
	label: string,
	fn: () => Promise<boolean | void>,
	detail?: () => string | Promise<string>,
): Promise<LogEntry> {
	try {
		const ok = await fn();
		return { label, ok: ok !== false, detail: detail ? await detail() : "ok" };
	} catch (e: any) {
		return { label, ok: false, detail: e?.message ?? String(e) };
	}
}

export function DiagnosticsPanel() {
	const [logs, setLogs] = useState<LogEntry[]>([]);

	const runAll = async () => {
		const results: LogEntry[] = [];

		// Crypto
		results.push(
			await check(
				"globalThis.crypto",
				async () => !!globalThis.crypto,
				() => typeof globalThis.crypto,
			),
		);
		results.push(
			await check(
				"globalThis.CryptoKey",
				async () => !!(globalThis as any).CryptoKey,
				() => typeof (globalThis as any).CryptoKey,
			),
		);
		results.push(
			await check(
				"crypto.subtle",
				async () => !!globalThis.crypto?.subtle,
				() => typeof globalThis.crypto?.subtle,
			),
		);
		results.push(
			await check("subtle.generateKey(AES-GCM)", async () => {
				await crypto.subtle.generateKey({ name: "AES-GCM", length: 256 }, true, [
					"encrypt",
					"decrypt",
				]);
			}),
		);
		results.push(
			await check("subtle.generateKey(ECDSA P-256)", async () => {
				await crypto.subtle.generateKey({ name: "ECDSA", namedCurve: "P-256" }, true, ["sign"]);
			}),
		);

		// Resolvers
		results.push(
			await check(
				"handleResolver",
				async () => typeof (handleResolver as any)?.resolve === "function",
			),
		);
		results.push(
			await check(
				"didDocumentResolver",
				async () => typeof (didDocumentResolver as any)?.resolve === "function",
			),
		);
		results.push(
			await check(
				"identityResolver",
				async () => typeof (identityResolver as any)?.resolve === "function",
			),
		);

		// Live resolution
		results.push(
			await check(
				"handleResolver.resolve(deniz.blue)",
				async () => {
					const did = await handleResolver.resolve("deniz.blue" as any);
					return !!did;
				},
				async () => {
					const did = await handleResolver.resolve("deniz.blue" as any);
					return did;
				},
			),
		);

		results.push(
			await check(
				"identityResolver.resolve(deniz.blue)",
				async () => {
					const r = await identityResolver.resolve("deniz.blue" as any);
					return !!r?.pds;
				},
				async () => {
					const r = await identityResolver.resolve("deniz.blue" as any);
					return `did=${r?.did} pds=${r?.pds}`;
				},
			),
		);

		// OAuth wiring
		results.push(
			await check("AtOAuthClient.resolver.actorResolver.resolve", async () => {
				const resolver = (AtOAuthClient as any).resolver;
				const ar = resolver?.actorResolver;
				return typeof ar?.resolve === "function";
			}),
		);

		results.push(
			await check(
				"resolveFromIdentity(deniz.blue)",
				async () => {
					const resolver = (AtOAuthClient as any).resolver;
					const r = await resolver.resolveFromIdentity("deniz.blue");
					return !!r?.identity?.pds;
				},
				async () => {
					const resolver = (AtOAuthClient as any).resolver;
					const r = await resolver.resolveFromIdentity("deniz.blue");
					return `did=${r?.identity?.did} pds=${r?.identity?.pds}`;
				},
			),
		);

		// Network
		results.push(
			await check(
				"fetch(public.api.bsky.app)",
				async () => {
					const res = await fetch(
						"https://public.api.bsky.app/xrpc/app.bsky.actor.getProfile?actor=bsky.app",
					);
					return res.ok;
				},
				async () => {
					const res = await fetch(
						"https://public.api.bsky.app/xrpc/app.bsky.actor.getProfile?actor=bsky.app",
					);
					const body = await res.json();
					return `status=${res.status} handle=${body?.handle ?? "n/a"}`;
				},
			),
		);

		setLogs(results);
	};

	const allPassed = logs.length > 0 && logs.every((l) => l.ok);

	return (
		<Box gap="xs">
			<AsyncButton fn={runAll}>
				{({ loading, onPress }) => (
					<Button onPress={onPress} loading={loading} justify="flex-start">
						Run Diagnostics
					</Button>
				)}
			</AsyncButton>

			{allPassed && (
				<Box bg={Colors.BackgroundLight} p="xs" radius={4}>
					<Text fz={FontSize.xs} c={Colors.LightGreen}>
						✓ Nothing out of the ordinary
					</Text>
				</Box>
			)}

			{logs
				.filter((l) => !l.ok)
				.map((entry, i) => (
					<Box key={i} bg="#3a1520" p="xs" radius={4}>
						<Text fz={FontSize.xs} fw="bold" c={Colors.Red7}>
							✗ {entry.label}
						</Text>
						<Text fz={10} c="TextDimmed" selectable>
							{entry.detail}
						</Text>
					</Box>
				))}
		</Box>
	);
}
