import { Fiber, traverseFiber } from "its-fine";
import {
	Component,
	ComponentType,
	createContext,
	Fragment,
	PropsWithChildren,
	use,
	useContext,
	useId,
	useMemo,
} from "react";

export const FiberHandleContext = createContext<Fiber | null>(null);

export class FiberHandle extends Component<PropsWithChildren> {
	private _reactInternals!: Fiber;

	render() {
		return (
			<FiberHandleContext value={this._reactInternals}>{this.props.children}</FiberHandleContext>
		);
	}
}

export function useFiber(): Fiber<null> | null {
	const root = useContext(FiberHandleContext);
	const id = useId();
	const fiber =
		useMemo(() => {
			if (root === null) return null;

			for (const maybeFiber of [root, root?.alternate]) {
				if (!maybeFiber) continue;
				const fiber = traverseFiber<null>(maybeFiber, false, (node) => {
					let state = node.memoizedState;
					while (state) {
						if (state.memoizedState === id) return true;
						state = state.next;
					}
				});
				if (fiber) return fiber;
			}
		}, [root, id]) ?? null;

	return fiber;
}

const REACT_CONTEXT_TYPE = Symbol.for("react.context");

const isContext = <T,>(type: unknown): type is React.Context<T> =>
	type !== null &&
	typeof type === "object" &&
	"$$typeof" in type &&
	type.$$typeof === REACT_CONTEXT_TYPE;

export const useContextMap = () => {
	const fiber = useFiber();

	const contextMap = new Map<any, any>();

	if (!fiber) return contextMap;

	let node: typeof fiber | null = fiber;
	while (node) {
		const context = node.type;
		if (context === FiberHandleContext) break;
		if (isContext(context) && !contextMap.has(context)) {
			const value = use(context);
			contextMap.set(context, value);
		}
		node = node.return!;
	}

	return contextMap;
};

export const useContextBridge = () => {
	const contextMap = useContextMap();

	const ContextBridge = useMemo(
		() =>
			Array.from(contextMap.entries()).reduce(
				(Prev: ComponentType<PropsWithChildren>, [Context, value]) => {
					return ({ children }: PropsWithChildren) => (
						<Prev>
							<Context value={value}>{children}</Context>
						</Prev>
					);
				},
				Fragment,
			),
		[contextMap],
	);

	return ContextBridge;
};
