-- Default lifecycle states
INSERT OR IGNORE INTO report_states (name, display_order, is_initial, is_terminal) VALUES ('Queued', 1, 1, 0);
INSERT OR IGNORE INTO report_states (name, display_order, is_initial, is_terminal) VALUES ('Assigned', 2, 0, 0);
INSERT OR IGNORE INTO report_states (name, display_order, is_initial, is_terminal) VALUES ('Initial Edit', 3, 0, 0);
INSERT OR IGNORE INTO report_states (name, display_order, is_initial, is_terminal) VALUES ('Review', 4, 0, 0);
INSERT OR IGNORE INTO report_states (name, display_order, is_initial, is_terminal) VALUES ('To Region', 5, 0, 0);
INSERT OR IGNORE INTO report_states (name, display_order, is_initial, is_terminal) VALUES ('Regional Review', 6, 0, 0);
INSERT OR IGNORE INTO report_states (name, display_order, is_initial, is_terminal) VALUES ('Completed', 7, 0, 1);

-- Default transitions
INSERT OR IGNORE INTO state_transitions (from_state_id, to_state_id) VALUES (1, 2);
INSERT OR IGNORE INTO state_transitions (from_state_id, to_state_id) VALUES (2, 3);
INSERT OR IGNORE INTO state_transitions (from_state_id, to_state_id) VALUES (3, 4);
INSERT OR IGNORE INTO state_transitions (from_state_id, to_state_id) VALUES (4, 5);
INSERT OR IGNORE INTO state_transitions (from_state_id, to_state_id) VALUES (5, 6);
INSERT OR IGNORE INTO state_transitions (from_state_id, to_state_id) VALUES (6, 7);

-- Sample editors
INSERT OR IGNORE INTO editors (name, email, calendar_id) VALUES ('Alice Johnson', 'alice.johnson@example.com', 'alice.johnson@example.com');
INSERT OR IGNORE INTO editors (name, email, calendar_id) VALUES ('Bob Smith', 'bob.smith@example.com', 'bob.smith@example.com');
INSERT OR IGNORE INTO editors (name, email, calendar_id) VALUES ('Carol Davis', 'carol.davis@example.com', 'carol.davis@example.com');
