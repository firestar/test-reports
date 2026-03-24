-- Default lifecycle states
INSERT INTO report_states (name, display_order, is_initial, is_terminal) VALUES ('Queued', 1, TRUE, FALSE);
INSERT INTO report_states (name, display_order, is_initial, is_terminal) VALUES ('Assigned', 2, FALSE, FALSE);
INSERT INTO report_states (name, display_order, is_initial, is_terminal) VALUES ('Initial Edit', 3, FALSE, FALSE);
INSERT INTO report_states (name, display_order, is_initial, is_terminal) VALUES ('Review', 4, FALSE, FALSE);
INSERT INTO report_states (name, display_order, is_initial, is_terminal) VALUES ('To Region', 5, FALSE, FALSE);
INSERT INTO report_states (name, display_order, is_initial, is_terminal) VALUES ('Regional Review', 6, FALSE, FALSE);
INSERT INTO report_states (name, display_order, is_initial, is_terminal) VALUES ('Completed', 7, FALSE, TRUE);

-- Default transitions
INSERT INTO state_transitions (from_state_id, to_state_id) VALUES (1, 2);
INSERT INTO state_transitions (from_state_id, to_state_id) VALUES (2, 3);
INSERT INTO state_transitions (from_state_id, to_state_id) VALUES (3, 4);
INSERT INTO state_transitions (from_state_id, to_state_id) VALUES (4, 5);
INSERT INTO state_transitions (from_state_id, to_state_id) VALUES (5, 6);
INSERT INTO state_transitions (from_state_id, to_state_id) VALUES (6, 7);

-- Sample editors
INSERT INTO editors (name, email, calendar_id) VALUES ('Alice Johnson', 'alice.johnson@example.com', 'alice.johnson@example.com');
INSERT INTO editors (name, email, calendar_id) VALUES ('Bob Smith', 'bob.smith@example.com', 'bob.smith@example.com');
INSERT INTO editors (name, email, calendar_id) VALUES ('Carol Davis', 'carol.davis@example.com', 'carol.davis@example.com');
