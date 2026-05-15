import { ActionIcon, Menu } from "@mantine/core";
import { modals } from "@mantine/modals";
import { IconBraces, IconBrandBluesky, IconCalendarPlus, IconEdit, IconFileImport, IconLink } from "@tabler/icons-react";
import { Link } from "@tanstack/react-router";
import { useRef } from "react";
import { useTasksStore } from "../../stores/useTasksStore";
import ICAL from "ical.js";
import { dbShortcuts } from "../../db/db-shortcuts";

export const AddEventMenu = () => {
	const icsFileInputRef = useRef<HTMLInputElement>(null);

	const handleICSFiles = (files: File[]) => {
		useTasksStore.getState().addTask({
			title: "Importing events from .ics file(s)",
		}, async () => {
			for (const file of files) {
				const text = await file.text();
				console.log("Read .ics file:", file.name, text);
				let jcalData = ICAL.parse(text)
				let comp = new ICAL.Component(jcalData);
				const vevents = comp.getAllSubcomponents("vevent");
				for (const vevent of vevents) {
					const veventString = vevent.toString();
					console.log("Parsed VEVENT:", veventString);
					
					await dbShortcuts.insertLocalEvent(veventString, { type: "ics" });
				};
			}
		});
	};

	return (
		<Menu>
			<Menu.Target>
				<ActionIcon
					size="input-md"
					color="gray"
					aria-label="Add Event"
				>
					<IconCalendarPlus />
					<input
						ref={icsFileInputRef}
						type="file"
						style={{ display: "none" }}
						accept=".ics"
						multiple
						onChange={(e) => {
							if (e.target.files) handleICSFiles(Array.from(e.target.files));
						}}
					/>
				</ActionIcon>
			</Menu.Target>
			<Menu.Dropdown>
				<Menu.Label>
					Add Event
				</Menu.Label>
				<Menu.Item
					leftSection={<IconEdit />}
					renderRoot={(props) => (
						<Link
							to="/new"
							{...props}
						/>
					)}
				>
					New
				</Menu.Item>
				<Menu.Item
					leftSection={<IconLink />}
					onClick={() => modals.openContextModal({
						modal: "ImportURLModal",
						innerProps: {},
					})}
				>
					From URL
				</Menu.Item>
				<Menu.Item
					leftSection={<IconBraces />}
					onClick={() => modals.openContextModal({
						modal: "ImportJSONModal",
						innerProps: {},
					})}
				>
					From JSON
				</Menu.Item>
				<Menu.Item
					leftSection={<IconFileImport />}
					onClick={() => icsFileInputRef.current?.click()}
				>
					From .ics
				</Menu.Item>
				<Menu.Item
					leftSection={<IconBrandBluesky />}
					onClick={() => modals.openContextModal({
						modal: "ImportAtModal",
						innerProps: {},
					})}
				>
					From Atmosphere
				</Menu.Item>
			</Menu.Dropdown>
		</Menu>
	)
};
