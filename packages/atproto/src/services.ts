import { CompositeDidDocumentResolver, CompositeHandleResolver, DohJsonHandleResolver, LocalActorResolver, PlcDidDocumentResolver, WebDidDocumentResolver, XrpcHandleResolver } from "@atcute/identity-resolver";

export const handleResolver = new CompositeHandleResolver({
	methods: {
		dns: new DohJsonHandleResolver({
			dohUrl: "https://cloudflare-dns.com/dns-query",
		}),
		http: new XrpcHandleResolver({
			serviceUrl: "https://public.api.bsky.app",
		}),
	},
});

export const didDocumentResolver = new CompositeDidDocumentResolver({
	methods: {
		plc: new PlcDidDocumentResolver(),
		web: new WebDidDocumentResolver(),
	},
});

export const identityResolver = new LocalActorResolver({
	handleResolver,
	didDocumentResolver,
});
