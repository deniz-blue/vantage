import { useCallback, useState } from "react";
import * as WebBrowser from "expo-web-browser";
import { AtOAuthClient } from "@vantage/atproto";
import { isDid, isHandle } from "@atcute/lexicons/syntax";
import type { AuthorizeTarget } from "@atcute/oauth-node-client";
import { Box } from "../base/Box";
import { Text } from "../base/Text";
import { TextInput } from "../base/input/TextInput";
import { Button } from "../base/button/Button";
import { Colors } from "../../theme/colors";
import { FontSize } from "../../theme/sizing";

export const AtprotoSignInSheetContent = ({ onClose }: { onClose?: () => void }) => {
	const [identifier, setIdentifier] = useState("");
	const [error, setError] = useState<Error | null>(null);
	const [loading, setLoading] = useState(false);

	const handleAuthorize = useCallback(async () => {
		setError(null);
		setLoading(true);

		try {
			const oauth = AtOAuthClient;
			const target: AuthorizeTarget =
				isDid(identifier) || isHandle(identifier)
					? { type: "account", identifier }
					: { type: "pds", serviceUrl: identifier };

			console.log("[SignIn] authorizing:", identifier);
			const { url, stateId } = await oauth.authorize({ target });
			console.log("[SignIn] stateId:", stateId, "url:", url.toString());
			await WebBrowser.openAuthSessionAsync(url.toString());
			onClose?.();
		} catch (err) {
			setError(err as Error);
		} finally {
			setLoading(false);
		}
	}, [identifier, onClose]);

	return (
		<Box gap="md">
			<Box gap="sm" align="center">
				<Text fz={FontSize.h1} fw="bold">
					Sign In to AT Protocol
				</Text>
			</Box>

			<TextInput
				label="Handle"
				placeholder="alice.bsky.social"
				value={identifier}
				onChangeText={setIdentifier}
				autoCapitalize="none"
				autoCorrect={false}
			/>

			{error && (
				<Box p="sm" bg={Colors.Red + "11"} style={{ borderRadius: 8 }}>
					<Text fz={FontSize.sm} c={Colors.Red}>
						{error.message}
					</Text>
					<Text fz={FontSize.sm} c={Colors.Red}>
						{error.stack}
					</Text>
				</Box>
			)}

			<Box direction="row" gap={8} justify="flex-end">
				<Button onPress={onClose}>Cancel</Button>
				<Button
					variant="primary"
					onPress={handleAuthorize}
					loading={loading}
					disabled={!identifier.trim()}
				>
					{loading ? "Signing in…" : "Sign In"}
				</Button>
			</Box>
		</Box>
	);
};
