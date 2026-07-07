import { ComponentType, PropsWithChildren, ComponentProps } from "react";

export const ComponentStack = <TStack extends readonly ComponentType<any>[]>({
	children,
	stack,
}: PropsWithChildren<{
	stack: {
		[I in keyof TStack]: [TStack[I], ComponentProps<TStack[I]>];
	};
}>) => {
	return stack.reduceRight(
		(prev, [Component, props]) => <Component {...props}>{prev}</Component>,
		children,
	);
};
