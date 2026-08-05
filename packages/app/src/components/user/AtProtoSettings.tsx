import { AtOAuthClient, useAtAccounts, useAtClient } from "@vantage/atproto";
import { InputWrapper } from "../base/input/InputWrapper";
import { Box } from "../base/Box";
import { AtprotoSignInSheetContent } from "../app/AtprotoSignInSheet";
import { ButtonSheet } from "../app/ButtonSheet";
import { AtUserCard } from "./AtUserCard";
import { AtprotoDid } from "@atcute/lexicons/syntax";
import { AsyncButton } from "../base/button/AsyncButton";
import { Client } from "@atcute/client";

export const AtProtoSettings = () => {
	const accounts = useAtAccounts((s) => s.accounts);
	const activeDid = useAtClient((s) => s.session?.did);
	const accountList = Object.values(accounts);

	const handleSignIn = async (did: AtprotoDid) => {
		console.log("Signing in with DID:", did);
		const session = await AtOAuthClient.restore(did);
		const client = new Client({ handler: session });
		useAtClient.setState({ client, session });
		console.log("Signed in with DID:", did);
	};

	return (
		<InputWrapper label="AT Protocol Accounts">
			{accountList.length > 0 ? (
				<Box gap="xs">
					{accountList.map((account) => (
						<AsyncButton key={account.did} fn={() => handleSignIn(account.did)}>
							{({ loading, onPress }) => (
								<AtUserCard
									did={account.did}
									active={account.did === activeDid}
									loading={loading}
									onPress={onPress}
									onMenu={() => {}}
								/>
							)}
						</AsyncButton>
					))}
				</Box>
			) : null}

			<ButtonSheet
				sheet={(ref) => <AtprotoSignInSheetContent onClose={() => ref.current?.dismiss()} />}
			>
				{accountList.length > 0 ? "Add Account" : "Sign In"}
			</ButtonSheet>
		</InputWrapper>
	);
};
