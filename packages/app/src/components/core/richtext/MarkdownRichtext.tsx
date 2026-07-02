import { Colors } from "../../../theme/colors";
import { Font, FontSize } from "../../../theme/sizing";
import { EnrichedMarkdownText } from "react-native-enriched-markdown";

export interface MarkdownRichtextProps {
	content: string;
}

export const MarkdownRichtext = ({ content }: MarkdownRichtextProps) => {
	return (
		<EnrichedMarkdownText
			markdown={content}
			markdownStyle={{
				paragraph: {
					fontFamily: Font.Default,
					fontSize: FontSize.md,
					color: Colors.Text,
				},
			}}
		/>
	);
};
