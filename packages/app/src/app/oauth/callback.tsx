import { useEffect, useState } from "react";
import { useLocalSearchParams, useRouter } from "expo-router";
import { AtOAuthClient, useAtAccounts, useAtClient } from "@vantage/atproto";
import { Client } from "@atcute/client";
import { Box } from "../../components/base/Box";
import { Text } from "../../components/base/Text";
import { Button } from "../../components/base/button/Button";
import { FontSize } from "../../theme/sizing";
import { Colors } from "../../theme/colors";
import { AtprotoDid } from "@atcute/lexicons/syntax";
import { EmptyState } from "../../components/base/EmptyState";

export default function OAuthCallback() {
	const params = useLocalSearchParams();
	const router = useRouter();
	const [error, setError] = useState<Error | null>(null);

	useEffect(() => {
		if (error) return;

		(async () => {
			try {
				const oauth = AtOAuthClient;
				const allParams = new URLSearchParams();
				for (const [key, value] of Object.entries(params)) {
					if (typeof value === "string") allParams.set(key, value);
				}

				console.log("[OAuthCallback] exchanging code…");
				const { session } = await oauth.callback(allParams);
				console.log("[OAuthCallback] session created, DID:", session.sub);

				const client = new Client({ handler: session });
				useAtClient.setState({ client, session });

				const info = await session.getTokenInfo();
				useAtAccounts.getState().addAccount({
					did: session.sub as AtprotoDid,
					pds: info.aud,
					lastActiveAt: Date.now(),
				});

				console.log("[OAuthCallback] success, navigating to tabs");
				router.replace("/");
			} catch (err: any) {
				console.error("[OAuthCallback] callback failed:", err, err.stack);
				setError(err as Error);
			}
		})();
	}, [params, error]);

	if (error) {
		return (
			<Box flex={1} justify="center" align="center" p="lg" gap="md">
				<Text fz={FontSize.h1} fw="bold" c={Colors.Red7}>
					Authentication Failed
				</Text>
				<Text fz={FontSize.md} c={Colors.Red7} ta="center">
					{Object.keys(params).join(",")}
				</Text>
				<Text c="TextDimmed" ta="center" style={{ fontFamily: "monospace", fontSize: 12 }}>
					{error.toString()}
					{error.stack}
				</Text>
				<Button onPress={() => router.replace("/")}>Back to App</Button>
			</Box>
		);
	}

	return <EmptyState fill loading message="Signing in..." />;
}
