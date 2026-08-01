import { PropsWithChildren, useRef } from "react";
import { JSX } from "react/jsx-runtime";
import { Sheet, SheetRef } from "../base/sheet/Sheet";
import { Button, ButtonProps } from "../base/button/Button";

export const ButtonSheet = ({
	sheet,
	...props
}: PropsWithChildren<
	{
		sheet: JSX.Element;
	} & ButtonProps
>) => {
	const ref = useRef<SheetRef>(null);

	return (
		<>
			<Button
				{...props}
				onPress={(e) => {
					ref.current?.present();
					props.onPress?.(e);
				}}
			/>

			<Sheet ref={ref}>{sheet}</Sheet>
		</>
	);
};
