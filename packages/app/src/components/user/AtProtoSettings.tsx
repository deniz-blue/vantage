import { useAtAccounts, useAtClient } from "@vantage/atproto";
import { InputWrapper } from "../base/input/InputWrapper";
import { Box } from "../base/Box";
import { AtprotoSignInSheetContent } from "../app/AtprotoSignInSheet";
import { ButtonSheet } from "../app/ButtonSheet";
import { AtUserCard } from "./AtUserCard";
import { AtprotoDid } from "@atcute/lexicons/syntax";
import { AsyncButton } from "../base/button/AsyncButton";
import { ActionButtonList } from "../actions/ActionButton";
import { Action } from "../actions/action";
import { useEffect, useRef, useState } from "react";
import { Sheet, SheetRef } from "../base/sheet/Sheet";

export const AtProtoSettings = () => {
	const [actionForDid, setActionForDid] = useState<AtprotoDid | null>(null);
	const sheet = useRef<SheetRef>(null);
	const accounts = useAtAccounts((s) => s.accounts);
	const activeDid = useAtClient((s) => s.session?.did);
	const accountList = Object.values(accounts);

	const handleSignIn = async (did: AtprotoDid) => {
		await useAtClient.getState().signIn(did);
	};

	useEffect(() => {
		if (actionForDid) sheet.current?.present();
		else sheet.current?.dismiss();
	}, [actionForDid]);

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
									onMenu={() => setActionForDid(account.did)}
									onLongPress={() => setActionForDid(account.did)}
								/>
							)}
						</AsyncButton>
					))}
				</Box>
			) : null}

			<Sheet ref={sheet} onDidDismiss={() => setActionForDid(null)}>
				{actionForDid && (
					<AtUserSheetContent did={actionForDid} onClose={() => setActionForDid(null)} />
				)}
			</Sheet>

			<ButtonSheet
				sheet={(ref) => <AtprotoSignInSheetContent onClose={() => ref.current?.dismiss()} />}
			>
				{accountList.length > 0 ? "Add Account" : "Sign In"}
			</ButtonSheet>
		</InputWrapper>
	);
};

export const AtUserSheetContent = ({ did, onClose }: { did: AtprotoDid; onClose?: () => void }) => {
	const activeDid = useAtClient((s) => s.session?.did);

	const actions: Action[] = [];

	actions.push({
		label: "BlueSky Profile",
		type: "link",
		url: `https://bsky.app/profile/${did}`,
	});

	if (did === activeDid)
		actions.push({
			label: "Sign Out",
			type: "fn",
			onRun: () => {
				useAtClient.setState({ client: null, session: null });
			},
		});
	else
		actions.push({
			label: "Sign In",
			type: "fn",
			onRun: async () => {
				await useAtClient.getState().signIn(did);
			},
		});

	actions.push({
		label: "Remove Account",
		type: "fn",
		danger: true,
		onRun: () => {
			useAtAccounts.getState().removeAccount(did);
			if (useAtClient.getState().session?.did === did)
				useAtClient.setState({ client: null, session: null });
			onClose?.();
		},
	});

	return <ActionButtonList actions={actions} />;
};
