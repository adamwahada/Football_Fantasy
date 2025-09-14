import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminDashboardHistoryComponent } from './admin-dashboard-history.component';

describe('AdminDashboardHistoryComponent', () => {
  let component: AdminDashboardHistoryComponent;
  let fixture: ComponentFixture<AdminDashboardHistoryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminDashboardHistoryComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AdminDashboardHistoryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
