import { Badge, Menu } from "@mantine/core";
import { modals } from "@mantine/modals";
import { schema } from "@vantage/db";

export const TagBadge = ({
	tag,
	readonly,
}: {
	tag: schema.Tag;
	readonly?: boolean;
}) => {
	return (
		<Menu withinPortal disabled={readonly}>
			<Menu.Target>
				<Badge
					tt="unset"
					color={tag.color ?? "gray"}
					variant="light"
					style={{ cursor: "pointer", textWrap: "nowrap", textOverflow: "clip" }}
				>
					{tag.name}
				</Badge>
			</Menu.Target>
			<Menu.Dropdown>
				<Menu.Label>
					{tag.name}
				</Menu.Label>
				<Menu.Item
					onClick={() => modals.openContextModal({
						modal: "EditTagModal",
						innerProps: { tagId: tag.id },
					})}
				>
					Edit Tag
				</Menu.Item>
			</Menu.Dropdown>
		</Menu>
	);
};
