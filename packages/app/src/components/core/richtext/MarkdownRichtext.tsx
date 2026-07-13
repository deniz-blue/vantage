import { Colors } from "../../../theme/colors";
import { Font, FontSize } from "../../../theme/sizing";
import { EnrichedMarkdownText, MarkdownStyle } from "react-native-enriched-markdown";

export interface MarkdownRichtextProps {
	content: string;
}

const fontFamily = Font.Default;
const fontSize = FontSize.md;
const color = Colors.Text;

const markdownStyle: MarkdownStyle = {
	paragraph: {
		fontFamily,
		fontSize,
		color,
	},
	blockquote: {
		fontFamily,
		fontSize,
		color,
	},
	em: {
		fontFamily,
		color,
	},
	list: {
		bulletColor: color,
		color,
		fontFamily,
		fontSize,
	},
	h1: {
		fontFamily,
		color,
	},
	h2: {
		fontFamily,
		color,
	},
	h3: {
		fontFamily,
		color,
	},
	h4: {
		fontFamily,
		color,
	},
	h5: {
		fontFamily,
		color,
	},
	h6: {
		fontFamily,
		color,
	},
};

export const MarkdownRichtext = ({ content }: MarkdownRichtextProps) => {
	return <EnrichedMarkdownText markdown={content} markdownStyle={markdownStyle} />;
};
