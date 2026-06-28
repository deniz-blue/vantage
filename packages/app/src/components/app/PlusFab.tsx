import { Fragment, useState } from "react";
import { Fab } from "../base/Fab";
import { Spacing } from "../../theme/spacing";
import { IconPlus } from "@tabler/icons-react-native";
import { usePathname, useRouter } from "expo-router";

export const PlusFab = () => {
	const router = useRouter();
	const path = usePathname();

	const show = path === "/" || path === "/list";

	if (!show) return null;

	return (
		<Fragment>
			<Fab
				wrapperProps={{ style: { bottom: 56 + Spacing.md } }}
				icon={<IconPlus color="#fff" />}
				actions={[
					{
						label: "Create",
						onPress: () => {
							router.push("/new");
						},
					},
					{
						label: "Import",
					}
				]}
			/>
		</Fragment>
	);
};
