import { Stack, Text } from "@mantine/core";
import { SmallTitle } from "../../base/SmallTitle";
import { IconAt, IconBraces, IconDatabase, IconExternalLink, IconWorld } from "@tabler/icons-react";
import { BaseSnippet } from "../../Snippet";
import { EventLinkButtonBase } from "../link/EventLinkButtonBase";
import { parseCanonicalResourceUri } from "@atcute/lexicons";
import { useAtProtoHandleQuery } from "../../../../lib/atproto/useAtProtoHandleQuery";
import { AtprotoDid } from "@atcute/lexicons/syntax";
import { SourceComponent } from "@evnt/schema";
import { useResolvedEvent } from "../../../../db/resolved-event";

export const EventDetailsSource = () => {
	const { data, source } = useResolvedEvent();

	const sourceComponents = data?.components?.filter((c): c is SourceComponent => c.$type === "directory.evnt.component.source") ?? [];

	const atUri = source.type == "at" ? parseCanonicalResourceUri(source.uri) : null;
	const handle = useAtProtoHandleQuery(atUri?.ok ? (atUri.value.repo as AtprotoDid) : undefined);

	return (
		<Stack gap={0} component="section">
			<SmallTitle>
				source
			</SmallTitle>

			{sourceComponents.map((component, index, array) => (
				<EventLinkButtonBase
					key={index}
					url={component.url}
				>
					<Text inherit span mr={4}>
						Source {array.length > 1 ? `${index + 1}` : ""}
					</Text> <IconExternalLink size={14} />
				</EventLinkButtonBase>
			))}

			{source.type === "local" && (
				<BaseSnippet icon={<IconDatabase />}>
					<Text inline>
						Browser/Device
					</Text>
				</BaseSnippet>
			)}

			{source.type === "at" && (
				<Stack gap={4}>
					<BaseSnippet icon={<IconAt />}>
						<Text inline>
							Atmosphere
						</Text>
					</BaseSnippet>

					{atUri && atUri.ok && (
						<EventLinkButtonBase
							url={`https://stargraph.link/user/${atUri.value.repo}`}
							leftSection={(
								<img
									src={`https://blobs.blue/${atUri.value.repo}/avatar-thumb`}
									alt={`Avatar of user ${atUri.value.repo}`}
									width={24}
									height={24}
									style={{ borderRadius: "50%" }}
								/>
							)}
						>
							<Text inherit span mr={4}>
								{handle.isSuccess ? handle.data : atUri.value.repo}
							</Text> <IconExternalLink size={14} />
						</EventLinkButtonBase>
					)}

					<EventLinkButtonBase
						url={`https://pds.ls/${source.uri}`}
						leftSection={<img src="https://pds.ls/favicon.ico" alt="PDSls" width={24} height={24} />}
					>
						<Text inherit span mr={4}>
							View on PDSls
						</Text> <IconExternalLink size={14} />
					</EventLinkButtonBase>
				</Stack>
			)}

			{source.type === "http" && (
				<Stack gap={4}>
					<BaseSnippet icon={<IconWorld />}>
						<Text inline>
							Internet
						</Text>
					</BaseSnippet>
					<EventLinkButtonBase
						leftSection={<IconBraces />}
						url={source.url}
					>
						<Text inherit span mr={4}>
							View raw data
						</Text> <IconExternalLink size={14} />
					</EventLinkButtonBase>
				</Stack>
			)}
		</Stack>
	)
};
