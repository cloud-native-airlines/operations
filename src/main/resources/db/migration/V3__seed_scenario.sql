-- Seed the one-aircraft grand-tour scenario. Airport coordinates match the
-- ADS-B seed so positions, schedule, and events correlate.

INSERT INTO airport (code, name, latitude, longitude, timezone) VALUES
    ('RST', 'Rochester International Airport',      43.9083,  -92.5000, 'America/Chicago'),
    ('MSP', 'Minneapolis-Saint Paul International', 44.8848,  -93.2223, 'America/Chicago'),
    ('SFO', 'San Francisco International',          37.6213, -122.3790, 'America/Los_Angeles'),
    ('ATL', 'Hartsfield-Jackson Atlanta International', 33.6407, -84.4277, 'America/New_York');

INSERT INTO aircraft (id, type, base_airport_code) VALUES
    ('N100CA', 'A320', 'RST');

INSERT INTO route (id, origin_code, destination_code, block_minutes) VALUES
    ('RST-MSP', 'RST', 'MSP',  40),
    ('MSP-SFO', 'MSP', 'SFO', 200),
    ('SFO-ATL', 'SFO', 'ATL', 250),
    ('ATL-RST', 'ATL', 'RST', 140);

INSERT INTO tour (id, name) VALUES
    ('grand-tour', 'Grand Tour');

INSERT INTO tour_leg (tour_id, seq, flight_number, route_id, turnaround_minutes) VALUES
    ('grand-tour', 1, 'CNA1000', 'RST-MSP', 60),
    ('grand-tour', 2, 'CNA0101', 'MSP-SFO', 60),
    ('grand-tour', 3, 'CNA0102', 'SFO-ATL', 60),
    ('grand-tour', 4, 'CNA1001', 'ATL-RST', 60);
