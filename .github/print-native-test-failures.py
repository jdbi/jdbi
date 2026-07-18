#!/usr/bin/env python3
"""Print a one-line summary of each failed testcase in JUnit XML report(s).

The GraalVM native test run emits thousands of passing lines and reports each
failure's actual cause (e.g. a MissingReflectionRegistrationError) a few hundred
lines above "BUILD FAILURE". graal-ci.yml runs this on failure so the real cause
is summarized at the end of the job log. Pass one or more report paths as args.
"""
import sys
import xml.etree.ElementTree as ET


def main(paths):
    for path in paths:
        for testcase in ET.parse(path).getroot().iter("testcase"):
            for child in testcase:
                if child.tag in ("failure", "error"):
                    detail = (child.get("message") or child.get("type") or "").strip()
                    first_line = detail.splitlines()[0] if detail else ""
                    print(f"{testcase.get('classname')}#{testcase.get('name')}: {first_line}")


if __name__ == "__main__":
    main(sys.argv[1:])
