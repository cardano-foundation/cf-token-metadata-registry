-- CIP-26 subjects are stored in lower case, like CIP-68 subjects and the asset units clients query with. A subject is
-- base16, so its case carries no meaning, but a registry file named in upper case was stored as written and could only
-- be found by a query in upper case (39 subjects on mainnet). New entries are lower-cased by the sync; this lower-cases
-- the ones already stored.

-- An entry in upper case whose lower-case twin is also stored would collide: the twin is kept (none on mainnet). The
-- logo goes first, it references the entry.
DELETE FROM logo l WHERE l.subject <> lower(l.subject) AND EXISTS (SELECT 1 FROM metadata t WHERE t.subject = lower(l.subject));
DELETE FROM metadata m WHERE m.subject <> lower(m.subject) AND EXISTS (SELECT 1 FROM metadata t WHERE t.subject = lower(m.subject));

-- logo.subject references metadata.subject, and an immediate foreign key is checked after each statement: it is dropped
-- for the two updates and added again
ALTER TABLE logo DROP CONSTRAINT IF EXISTS fk_logo_metadata_subject;
UPDATE metadata SET subject = lower(subject) WHERE subject <> lower(subject);
UPDATE logo SET subject = lower(subject) WHERE subject <> lower(subject);
ALTER TABLE logo ADD CONSTRAINT fk_logo_metadata_subject FOREIGN KEY (subject) REFERENCES metadata (subject);
