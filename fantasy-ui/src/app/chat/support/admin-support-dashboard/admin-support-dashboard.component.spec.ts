import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminSupportDashboardComponent } from './admin-support-dashboard.component';

describe('AdminSupportDashboardComponent', () => {
  let component: AdminSupportDashboardComponent;
  let fixture: ComponentFixture<AdminSupportDashboardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminSupportDashboardComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AdminSupportDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
