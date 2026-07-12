import { useResolvedEvent } from "@vantage/core";
import type { SplashMediaComponent } from "@evnt/types";
import { Image } from "../base/Image";
import { Box } from "../base/Box";

export const EventBackground = ({ dimmed = true }: { dimmed?: boolean }) => {
	const { data } = useResolvedEvent();

	const splash = data?.components?.find((c) => {
		if (c.$type !== "directory.evnt.component.splashMedia") return false;
		return (c as SplashMediaComponent).roles.includes("background");
	}) as SplashMediaComponent | undefined;

	const url = splash?.media?.sources[0]?.url;
	if (!url) return null;

	const dominantColor = splash?.media?.presentation?.dominantColor;

	return (
		<Box absoluteFill style={{ overflow: "hidden" }}>
			{dominantColor && <Box absoluteFill bg={dominantColor} />}
			<Image source={{ uri: url }} resizeMode="cover" absoluteFill />
			{dimmed && <Box absoluteFill bg="Black" op={0.8} />}
		</Box>
	);
};
