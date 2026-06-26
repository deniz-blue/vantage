import { Fiber, traverseFiber } from "its-fine";
import { Component, Context, createContext, PropsWithChildren, use, useContext, useId, useMemo, useState } from "react";

export const FiberHandleContext = createContext<Fiber | null>(null);

export class FiberHandle extends Component<PropsWithChildren> {
	private _reactInternals!: Fiber;

	render() {
		return (
			<FiberHandleContext value={this._reactInternals}>
				{this.props.children}
			</FiberHandleContext>
		);
	}
};

export function useFiber(): Fiber<null> | undefined {
	const root = useContext(FiberHandleContext);
	if (root === null) throw new Error('its-fine: useFiber must be called within a <FiberProvider />!');

	const id = useId()
	const fiber = useMemo(() => {
		for (const maybeFiber of [root, root?.alternate]) {
			if (!maybeFiber) continue
			const fiber = traverseFiber<null>(maybeFiber, false, (node) => {
				let state = node.memoizedState
				while (state) {
					if (state.memoizedState === id) return true
					state = state.next
				}
			})
			if (fiber) return fiber
		}
	}, [root, id])

	return fiber
}

const REACT_CONTEXT_TYPE = Symbol.for("react.context");

const isContext = <T,>(type: unknown): type is React.Context<T> =>
	type !== null && typeof type === 'object' && '$$typeof' in type && type.$$typeof === REACT_CONTEXT_TYPE;

export const useContextMap = () => {
	const root = useContext(FiberHandleContext);
	const fiber = useFiber();
	if (!root) throw new Error("Root FiberHandle missing");

	const [contextMap] = useState(() => new Map<Context<any>, any>())

	contextMap.clear()
	let node = fiber
	while (node) {
		const context = node.type
		if (context === FiberHandleContext) break;
		if (isContext(context) && !contextMap.has(context)) {
			contextMap.set(context, use(context));
		}

		node = node.return!
	}

	return contextMap
}

export const useContextBridge = () => {
	const contextMap = useContextMap();

	// console.log("useContextBridge", [...contextMap.values()]);

	const ContextBridge = useMemo(() => (
		contextMap.entries().reduce((Prev, [Context, value]) => {
			return ({ children }: PropsWithChildren) => (
				<Prev>
					<Context value={value}>
						{children}
					</Context>
				</Prev>
			);
		}, ({ children }: PropsWithChildren) => <>{children}</>)
	), [contextMap]);

	return ContextBridge;
};
