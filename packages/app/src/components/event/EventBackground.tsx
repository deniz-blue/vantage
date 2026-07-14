import { useResolvedEvent } from "@vantage/core";
import type { SplashMediaComponent } from "@evnt/types";
import { Image } from "expo-image";
import { Box } from "../base/Box";
import { useTranslator } from "../../hooks/useTranslator";
import { memo } from "react";

export const EventBackground = memo(({ dimmed = true }: { dimmed?: boolean }) => {
	const { data } = useResolvedEvent();
	const t = useTranslator();

	const splash = data?.components?.find((c) => {
		if (c.$type !== "directory.evnt.component.splashMedia") return false;
		return (c as SplashMediaComponent).roles.includes("background");
	}) as SplashMediaComponent | undefined;

	const url = splash?.media?.sources[0]?.url;
	if (!url) return null;

	const dominantColor = splash.media.presentation?.dominantColor;

	return (
		<Box absoluteFill style={{ overflow: "hidden" }}>
			{dominantColor && <Box absoluteFill bg={dominantColor} />}
			<Box
				component={Image}
				source={{ uri: url, blurhash: splash.media.presentation?.blurhash }}
				contentFit="cover"
				alt={splash.media.alt ? t(splash.media.alt) : undefined}
				absoluteFill
			/>
			{dimmed && <Box absoluteFill bg="Black" op={0.8} />}
		</Box>
	);
});
