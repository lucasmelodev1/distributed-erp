import "@testing-library/jest-dom/vitest"
import { cleanup } from "@testing-library/react"
import { afterEach } from "vitest"

afterEach(() => cleanup())

// jsdom lacks this; input-otp calls it from a timer.
document.elementFromPoint = () => null
