import { Fragment } from "react/jsx-runtime";
import { Colors } from "../../../theme/colors";
import { Font, FontSize, Radius } from "../../../theme/sizing";
import { EnrichedMarkdownText, MarkdownStyle } from "react-native-enriched-markdown";
import { memo, useState } from "react";
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
	table: {
		color: Colors.Text,
		headerBackgroundColor: Colors.Dark5,
		headerTextColor: Colors.Text,
		rowOddBackgroundColor: Colors.Dark6,
		rowEvenBackgroundColor: Colors.Dark6,
		borderColor: Colors.Dark7,
		borderRadius: Radius.sm,
	},
};

export const MarkdownRichtext = memo(({ content }: MarkdownRichtextProps) => {
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
});
