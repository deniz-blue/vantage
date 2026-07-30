import { db, schema } from "@vantage/db";

declare global {
	namespace Vantage {
		interface CredentialTypeMap {
			basic: {
				url: string;
				username: string;
				password: string;
			};
		}
	}
}

export const Credentials = new (class {
	async getAll(): Promise<Vantage.Credential[]> {
		return await db
			.select()
			.from(schema.credentials)
			.then((rows) => rows.map((row) => row.data));
	}
})();
