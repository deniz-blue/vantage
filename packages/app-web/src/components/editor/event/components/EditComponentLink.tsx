import { Input, SimpleGrid, Stack, TextInput } from "@mantine/core";
import { Deatom, DeatomOptional, type EditAtom } from "../../edit-atom";
import type { LinkComponent } from "@evnt/schema";
import { focusAtom } from "jotai-optics";
import { TranslationsInput } from "../../../base/input/TranslationsInput";
import { ClearableSwitch } from "../../../base/input/ClearableSwitch";
import { PartialDateInput } from "../../../base/input/PartialDateInput";
import { useMemo } from "react";

export const EditComponentLink = ({ data }: { data: EditAtom<LinkComponent> }) => {
	const urlAtom = useMemo(() => focusAtom(data, o => o.prop("url")), [data]);
	const nameAtom = useMemo(() => focusAtom(data, o => o.prop("name")), [data]);
	const disabledAtom = useMemo(() => focusAtom(data, o => o.prop("disabled").valueOr(false)), [data]);
	const opensAtAtom = useMemo(() => focusAtom(data, o => o.prop("opensAt")), [data]);
	const closesAtAtom = useMemo(() => focusAtom(data, o => o.prop("closesAt")), [data]);

	return (
		<Stack>
			<Deatom
				atom={urlAtom}
				component={TextInput}
				label="URL"
				placeholder="https://example.com"
				withAsterisk
				required
				type="url"
			/>

			<Stack gap={4}>
				<Stack gap={0}>
					<Input.Label>Link Name</Input.Label>
					<Input.Description>
						Name for the link, e.g. "Registration" or "Livestream". Optional, but can help users understand the purpose of the link.
					</Input.Description>
				</Stack>
				<DeatomOptional
					atom={nameAtom}
					component={TranslationsInput}
					set={{}}
					placeholder="Registration / Livestream / etc."
				/>
			</Stack>

			<Deatom
				component={ClearableSwitch}
				atom={disabledAtom}
				label="Always Disabled"
				color="red"
			/>

			<SimpleGrid type="container" cols={{ base: 1, "450px": 2 }}>
				{(["opensAt", "closesAt"] as const).map((field) => (
					<Stack gap={4} key={field}>
						<Stack gap={0}>
							<Input.Label>{field == "opensAt" ? "Opens at" : "Closes at"}</Input.Label>
							<Input.Description>
								{field == "opensAt" ? "When the link starts being available" : "When the link stops being available; such as the end of registrations"}
							</Input.Description>
						</Stack>
						<Deatom
							component={PartialDateInput}
							atom={field === "opensAt" ? opensAtAtom : closesAtAtom}
						/>
					</Stack>
				))}
			</SimpleGrid>
		</Stack>
	)
};
