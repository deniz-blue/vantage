import { Component, type ErrorInfo, type ReactNode } from "react";

export interface ErrorBoundaryProps {
	children: ReactNode;
	fallback?: (error: Error, reset: () => void) => ReactNode;
	onError?: (error: Error, info: ErrorInfo) => void;
}

export interface ErrorBoundaryState {
	error: Error | null;
}

export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
	state: ErrorBoundaryState = { error: null };

	static getDerivedStateFromError(error: Error): ErrorBoundaryState {
		return { error };
	}

	componentDidCatch(error: Error, info: ErrorInfo) {
		this.props.onError?.(error, info);
	}

	handleReset = () => {
		this.setState({ error: null });
	};

	render() {
		const { error } = this.state;
		if (error && this.props.fallback) return this.props.fallback(error, this.handleReset);
		return this.props.children;
	}
}
