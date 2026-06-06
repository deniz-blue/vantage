import { Box } from "../../components/base/Box";
import { Text } from "../../components/base/Text";
import { useEventListQuery } from "@vantage/core";

export default function List() {
	const query = useEventListQuery({});

	console.log(query.events);

	return (
		<Box>
			<Text>
				List
			</Text>
		</Box>
	)
}