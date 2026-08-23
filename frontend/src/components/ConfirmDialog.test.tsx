import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { ConfirmDialog } from "./ConfirmDialog";

describe("ConfirmDialog", () => {
  it("requires an explicit confirmation", async () => {
    const user = userEvent.setup();
    const confirm = vi.fn();
    render(<ConfirmDialog open title="Return this book?" description="The copy becomes available." confirmLabel="Confirm return" onCancel={vi.fn()} onConfirm={confirm} />);

    await user.click(screen.getByRole("button", { name: "Confirm return" }));
    expect(confirm).toHaveBeenCalledOnce();
  });

  it("supports keyboard escape", async () => {
    const user = userEvent.setup();
    const cancel = vi.fn();
    render(<ConfirmDialog open title="Return this book?" description="The copy becomes available." confirmLabel="Confirm return" onCancel={cancel} onConfirm={vi.fn()} />);

    await user.keyboard("{Escape}");
    expect(cancel).toHaveBeenCalledOnce();
  });
});
