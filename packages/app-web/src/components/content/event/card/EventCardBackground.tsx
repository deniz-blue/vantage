import type { SplashMediaComponent } from "@evnt/schema";
import classes from "./event-card.module.css";
import { OverLayer } from "../../../base/layout/OverLayer";
import { EvntMedia } from "../../../base/media/EvntMedia";
import { useResolvedEvent } from "@vantage/core";

export interface EventCardBackgroundProps {
	backgroundDim?: number;
	backgroundOpacity?: number;
};

export const EventCardBackground = (props: EventCardBackgroundProps) => {
	const { data } = useResolvedEvent();

	const backgroundMedia = data?.components
		?.find((c): c is SplashMediaComponent =>
			c.$type === "directory.evnt.component.splashMedia" && (c as SplashMediaComponent).roles.includes("background"))
		?.media;

	return (
		<OverLayer style={{ opacity: props.backgroundOpacity }}>
			{backgroundMedia && (
				<OverLayer>
					<EvntMedia
						media={backgroundMedia}
						objectFit="cover"
						coverElement={(
							<OverLayer
								className={classes.dim}
								style={{ "--dim": props.backgroundDim ?? (!backgroundMedia ? 0.05 : undefined) }}
							/>
						)}
					/>
				</OverLayer>
			)}
		</OverLayer>
	)
};
