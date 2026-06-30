import { Text } from "../../base/Text";
import { FontSize } from "../../../theme/sizing";

export interface MarkdownRichtextProps {
	content: string;
}

export const MarkdownRichtext = ({ content }: MarkdownRichtextProps) => (
	<Text fz={FontSize.md}>{content}</Text>
);
