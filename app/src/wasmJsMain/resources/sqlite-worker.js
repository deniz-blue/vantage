import sqlite3InitModule from "@sqlite.org/sqlite-wasm";

const databases = new Map();
const statements = new Map();
let nextId = 1;

const ready = sqlite3InitModule().then(async (sqlite3) => {
	const pool = await sqlite3.installOpfsSAHPoolVfs({ name: "vantage-pool" });
	return { sqlite3, pool };
});

function open({ pool }, { fileName }) {
	const database = new pool.OpfsSAHPoolDb(fileName.startsWith("/") ? fileName : `/${fileName}`);
	const databaseId = nextId++;
	databases.set(databaseId, database);
	return { databaseId };
}

function prepare({ sqlite3 }, { databaseId, sql }) {
	const statement = databases.get(databaseId).prepare(sql);
	const statementId = nextId++;
	statements.set(statementId, statement);
	return {
		statementId,
		parameterCount: statement.parameterCount,
		columnNames: Array.from({ length: statement.columnCount }, (_, index) =>
			sqlite3.capi.sqlite3_column_name(statement.pointer, index),
		),
	};
}

function step({ sqlite3 }, { statementId, bindings }) {
	const statement = statements.get(statementId);
	statement.reset(true);
	if (bindings.length > 0) statement.bind(bindings);
	const rows = [];
	let columnTypes = [];
	while (statement.step()) {
		if (rows.length === 0) {
			columnTypes = Array.from({ length: statement.columnCount }, (_, index) =>
				sqlite3.capi.sqlite3_column_type(statement.pointer, index),
			);
		}
		rows.push(statement.get([]));
	}
	statement.reset();
	return { rows, columnTypes };
}

function close({ statementId, databaseId }) {
	if (statementId != null) {
		statements.get(statementId)?.finalize();
		statements.delete(statementId);
	}
	if (databaseId != null) {
		databases.get(databaseId)?.close();
		databases.delete(databaseId);
	}
}

onmessage = async ({ data: { id, data } }) => {
	try {
		const runtime = await ready;
		switch (data.cmd) {
			case "open":
				postMessage({ id, data: open(runtime, data) });
				break;
			case "prepare":
				postMessage({ id, data: prepare(runtime, data) });
				break;
			case "step":
				postMessage({ id, data: step(runtime, data) });
				break;
			case "close":
				close(data);
				break;
			default:
				postMessage({ id, error: `Unknown command ${data.cmd}` });
		}
	} catch (error) {
		postMessage({ id, error: error?.message ?? String(error) });
	}
};
