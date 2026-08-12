import { useResolvedEvent } from "@vantage/core";
import type { SplashMediaComponent } from "@evnt/types";
import { Image } from "expo-image";
import { Box, BoxProps } from "../base/Box";
import { useTranslator } from "../../hooks/useTranslator";
import { memo } from "react";
import { Media } from "@evnt/types";

export const EventBackground = memo(({ dimmed = true }: { dimmed?: boolean }) => {
	const { data } = useResolvedEvent();

	const splash = data?.components?.find((c) => {
		if (c.$type !== "directory.evnt.component.splashMedia") return false;
		return (c as SplashMediaComponent).roles.includes("background");
	}) as SplashMediaComponent | undefined;

	const url = splash?.media?.sources[0]?.url;
	if (!url) return null;

	return (
		<Box absoluteFill style={{ overflow: "hidden" }}>
			<OpenEvntImage media={splash.media} absoluteFill />
			{dimmed && <Box absoluteFill bg="Black" op={0.8} />}
		</Box>
	);
});

export interface OpenEvntImageProps extends BoxProps {
	media: Media;
}

export const OpenEvntImage = ({ media, ...props }: OpenEvntImageProps) => {
	const t = useTranslator();
	const url = media.sources[0]?.url;
	if (!url) return null;

	const dominantColor = media.presentation?.dominantColor;

	return (
		<Box {...props}>
			{dominantColor && <Box absoluteFill bg={dominantColor} />}
			<Box
				component={Image}
				source={{ uri: url, blurhash: media.presentation?.blurhash }}
				contentFit="cover"
				alt={media.alt ? t(media.alt) : undefined}
				absoluteFill
			/>
		</Box>
	);
};
