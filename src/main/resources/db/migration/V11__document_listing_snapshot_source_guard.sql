COMMENT ON COLUMN listing_snapshots.source_snapshot IS
    'Full live listing JSON when <=32768 characters; otherwise Java stores a trimmed summary object.';
