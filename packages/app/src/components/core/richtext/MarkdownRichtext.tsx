import { Fragment } from "react/jsx-runtime";
import { Colors } from "../../../theme/colors";
import { Font, FontSize } from "../../../theme/sizing";
import { EnrichedMarkdownText, MarkdownStyle } from "react-native-enriched-markdown";
import { useState } from "react";
import { ExternalLinkSheet } from "../ExternalLinkSheet";

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
	const [link, setLink] = useState<string | null>(null);

	return (
		<Fragment>
			<EnrichedMarkdownText
				markdown={content}
				markdownStyle={markdownStyle}
				onLinkPress={({ url }) => setLink(url)}
				onLinkLongPress={({ url }) => setLink(url)}
				flavor="github"
			/>

			<ExternalLinkSheet link={link} onClose={() => setLink(null)} />
		</Fragment>
	);
};
